package mb.fw.paradise.gateway.filter;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ApiMessageType;
import mb.fw.paradise.constants.ESBApiHeaderConstants;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.service.LoggingService;
import mb.fw.paradise.util.HttpHeaderUtil;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GatewayHeaderLoggingFilter implements GlobalFilter {

	private final Optional<JmsTemplate> jmsTemplate;
	private LoggingService loggingService;

	public GatewayHeaderLoggingFilter(Optional<JmsTemplate> jmsTemplate, LoggingService loggingService) {
		this.jmsTemplate = jmsTemplate;
		this.loggingService = loggingService;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		Set<URI> originalUris = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
		URI originalUri = (originalUris != null && !originalUris.isEmpty()) ? originalUris.iterator().next()
				: exchange.getRequest().getURI();
		URI requestUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
		ServerHttpRequest request = exchange.getRequest();
		HttpHeaders requestHeader = request.getHeaders();
		String interfaceId = HttpHeaderUtil.getHeader(requestHeader, ESBApiHeaderConstants.INTERFACE_ID);
		String messageType = HttpHeaderUtil.getHeader(requestHeader, ESBApiHeaderConstants.API_MESSAGE_TYPE);
		String transactionId = HttpHeaderUtil.getHeader(requestHeader, ESBApiHeaderConstants.TRANSACTION_ID);
		MDC.put("esbTxId", transactionId);
		log.info("Routing [{}] : '{}' ---> '{}'", messageType, originalUri, requestUri.toString());

		// ------------------------
		// 요청 header 로깅
		// ------------------------
		log.info("HTTP request header : {}", requestHeader);

		exchange.getAttributes().put(ESBApiHeaderConstants.INTERFACE_ID, interfaceId);
		exchange.getAttributes().put(ESBApiHeaderConstants.TRANSACTION_ID, transactionId);
		exchange.getAttributes().put(ESBApiHeaderConstants.API_MESSAGE_TYPE, messageType);
		exchange.getAttributes().put(ESBApiHeaderConstants.DATA_COUNT,
				HttpHeaderUtil.getIntHeader(requestHeader, ESBApiHeaderConstants.DATA_COUNT));
		exchange.getAttributes().put(ESBApiHeaderConstants.SEND_SYSTEM_CODE,
				HttpHeaderUtil.getHeader(requestHeader, ESBApiHeaderConstants.SEND_SYSTEM_CODE));
		exchange.getAttributes().put(ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE,
				HttpHeaderUtil.getHeader(requestHeader, ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE));
		exchange.getAttributes().put(ESBApiHeaderConstants.ESB_STATUS_CODE,
				HttpHeaderUtil.getHeader(requestHeader, ESBApiHeaderConstants.ESB_STATUS_CODE));
		String statusMessage = HttpHeaderUtil.getHeader(requestHeader, ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
		if (HttpHeaderUtil.isBase64(statusMessage)) {
			statusMessage = new String(Base64.getDecoder().decode(statusMessage), StandardCharsets.UTF_8);
		}
		exchange.getAttributes().put(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, statusMessage);

		ApiMessageType apiMessagType = ApiMessageType
				.valueOf((String) exchange.getAttributes().get(ESBApiHeaderConstants.API_MESSAGE_TYPE));
		if (apiMessagType.equals(ApiMessageType.SYNC)) {
			processByHeaders(exchange.getAttributes(), true);
		}

		// ------------------------
		// 응답 header 로깅
		// ------------------------
		ServerHttpResponse response = exchange.getResponse();
		response.beforeCommit(() -> Mono.deferContextual(ctx -> {
			if (transactionId != null) {
				MDC.put("esbTxId", transactionId);
			}
			try {
				HttpHeaders headers = response.getHeaders();
				HttpStatus status = response.getStatusCode();
				log.info("HTTP response header : [{}] {}", status.toString(), headers);
			} finally {
				MDC.remove("esbTxId");
			}
			return Mono.empty();
		}));
		
		return chain.filter(exchange).doOnError(ex -> {
			log.error("target service call error!! {}", ex.getMessage(), ex);
			exchange.getAttributes().put(ESBApiHeaderConstants.ESB_STATUS_CODE, ESBStatusConstants.FAIL);
			exchange.getAttributes().put(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, ex.getMessage());
			processByHeaders(exchange.getAttributes(), false);
		}).doOnSuccess(Void -> {
			if (apiMessagType.equals(ApiMessageType.SYNC)) {
				String encoded = exchange.getResponse().getHeaders().getFirst(ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
				if (encoded != null) {
					String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
					log.debug("Decoded 'ESB_STATUS_MESSAGE' value: {}", decoded);
					exchange.getAttributes().put(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, decoded);
				}
				exchange.getAttributes().put(ESBApiHeaderConstants.ESB_STATUS_CODE, HttpHeaderUtil
						.getHeader(exchange.getResponse().getHeaders(), ESBApiHeaderConstants.ESB_STATUS_CODE));
				processByHeaders(exchange.getAttributes(), false);
			} else {
				processByHeaders(exchange.getAttributes(), false);
			}
		}).doFinally(signal -> MDC.remove("esbTxId"));

	}

	private void processByHeaders(Map<String, Object> attributesMap, boolean isSyncRequest) {
		String interfaceId = (String) attributesMap.get(ESBApiHeaderConstants.INTERFACE_ID);
		String transactionId = (String) attributesMap.get(ESBApiHeaderConstants.TRANSACTION_ID);
		String sendSystemCode = (String) attributesMap.get(ESBApiHeaderConstants.SEND_SYSTEM_CODE);
		String receiveSystemCode = (String) attributesMap.get(ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE);
		String esbStatusCode = (String) attributesMap.get(ESBApiHeaderConstants.ESB_STATUS_CODE);
		String esbStatusMessage = (String) attributesMap.get(ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
		int dataCount = (Integer) attributesMap.get(ESBApiHeaderConstants.DATA_COUNT) == 0 ? 1
				: (Integer) attributesMap.get(ESBApiHeaderConstants.DATA_COUNT);
		ApiMessageType apiMessagType = ApiMessageType
				.valueOf((String) attributesMap.get(ESBApiHeaderConstants.API_MESSAGE_TYPE));

		// 동기 요청 메시지
		if (isSyncRequest) {
			jmsTemplate.ifPresent(jms -> {
				loggingService.asyncStartLogging(jms, interfaceId, transactionId, sendSystemCode, receiveSystemCode,
						dataCount);
			});
		}
		// 비동기 요청 메시지(오류시 응답 메시지도 같이)
		if (apiMessagType.equals(ApiMessageType.REQUEST)) {
			jmsTemplate.ifPresent(jms -> {
				loggingService.asyncStartLogging(jms, interfaceId, transactionId, sendSystemCode, receiveSystemCode,
						dataCount);
			});
			if (!esbStatusCode.isEmpty() && ESBStatusConstants.FAIL.equals(esbStatusCode))
				jmsTemplate.ifPresent(jms -> {
					loggingService.asyncEndLogging(jms, interfaceId, transactionId, dataCount, esbStatusCode,
							esbStatusMessage);
				});
		}
		// 비동기 응답 메시지
		else if (apiMessagType.equals(ApiMessageType.RESPONSE)) {
			jmsTemplate.ifPresent(jms -> {
				loggingService.asyncEndLogging(jms, interfaceId, transactionId, dataCount, esbStatusCode,
						esbStatusMessage);
			});
		}
		// 동기 응답 메시지
		else {
			jmsTemplate.ifPresent(jms -> {
				loggingService.asyncEndLogging(jms, interfaceId, transactionId, dataCount, esbStatusCode,
						esbStatusMessage);
			});
		}
	}
}
