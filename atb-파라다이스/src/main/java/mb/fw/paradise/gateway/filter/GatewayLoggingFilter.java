package mb.fw.paradise.gateway.filter;

import java.util.Optional;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ESBAPIHeaderConstants;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.service.LoggingService;
import mb.fw.paradise.util.HttpHeaderUtil;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(1)
public class GatewayLoggingFilter implements GlobalFilter {

	private final Optional<JmsTemplate> jmsTemplate;
	private LoggingService loggingService;

	public GatewayLoggingFilter(Optional<JmsTemplate> jmsTemplate, LoggingService loggingService) {
		this.jmsTemplate = jmsTemplate;
		this.loggingService = loggingService;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// 요청 로깅
		log.info("[gateway-logging-filter] Request: " + exchange.getRequest().getMethod() + " "
				+ exchange.getRequest().getURI());

		ServerHttpRequest request = exchange.getRequest();
		HttpHeaders headers = request.getHeaders();
		headers.forEach((key, valueList) -> {
			String joinedValues = String.join(",", valueList);
			log.debug("http header info -> {} : {}", key, joinedValues);
		});

		// 필요한 헤더 추출
		String interfaceId = HttpHeaderUtil.getHeader(headers, ESBAPIHeaderConstants.INTERFACE_ID);
		String transactionId = HttpHeaderUtil.getHeader(headers, ESBAPIHeaderConstants.TRANSACTION_ID);
		String sendSystemCode = HttpHeaderUtil.getHeader(headers, ESBAPIHeaderConstants.SEND_SYSTEM_CODE);
		String receiveSystemCode = HttpHeaderUtil.getHeader(headers, ESBAPIHeaderConstants.RECEIVE_SYSTEM_CODE);
		String esbStatusCode = HttpHeaderUtil.getHeader(headers, ESBAPIHeaderConstants.ESB_STATUS_CODE);
		String esbStatusMessage = HttpHeaderUtil.getHeader(headers, ESBAPIHeaderConstants.ESB_STATUS_MESSAGE);
		int totalCount = HttpHeaderUtil.getIntHeader(headers, ESBAPIHeaderConstants.TOTAL_COUNT);
		int errorCount = HttpHeaderUtil.getIntHeader(headers, ESBAPIHeaderConstants.ERROR_COUNT);
		String callBackPath = HttpHeaderUtil.getHeader(headers, ESBAPIHeaderConstants.CALL_BACK_PATH);

		// 후처리에 필요한 정보 저장
		exchange.getAttributes().put("interfaceId", interfaceId);
		exchange.getAttributes().put("transactionId", transactionId);
		exchange.getAttributes().put("sendSystemCode", sendSystemCode);
		exchange.getAttributes().put("receiveSystemCode", receiveSystemCode);
		exchange.getAttributes().put("esbStatusCode", esbStatusCode);
		exchange.getAttributes().put("esbStatusMessage", esbStatusMessage);
		exchange.getAttributes().put("totalCount", totalCount);
		exchange.getAttributes().put("errorCount", errorCount);
		exchange.getAttributes().put("callBackPath", callBackPath);

		// 동기 인터페이스 경우, 요청시에도 로깅 jms 송신
		if (!HttpHeaderUtil.getHeader(headers, ESBAPIHeaderConstants.CALL_SYNC).isEmpty()) {
			processByHeaders(exchange, null);
		}

		return chain.filter(exchange).doFinally(signalType -> {
			HttpStatus statusCode = exchange.getResponse().getStatusCode();
			log.info("[GatewayFilter] Response Status Code: {}", statusCode);

			if (statusCode != null && !statusCode.is2xxSuccessful()) {
				log.warn("[gateway-logging-filter] Non-200 Response Detected: {}", statusCode);
				String errorMessage = "요청 처리 오류 : [" + statusCode + "] " + statusCode.getReasonPhrase();
				processByHeaders(exchange, errorMessage);
			} else {
				processByHeaders(exchange, null);
			}
		});

	}

	private void processByHeaders(ServerWebExchange exchange, String errorMessage) {
		String interfaceId = (String) exchange.getAttribute("interfaceId");
		String transactionId = (String) exchange.getAttribute("transactionId");
		String sendSystemCode = (String) exchange.getAttribute("sendSystemCode");
		String receiveSystemCode = (String) exchange.getAttribute("receiveSystemCode");
		String esbStatusCode = (String) exchange.getAttribute("esbStatusCode");
		String esbStatusMessage = (String) exchange.getAttribute("esbStatusMessage");
		int totalCount = Integer.valueOf(exchange.getAttribute("totalCount"));
		int errorCount = Integer.valueOf(exchange.getAttribute("errorCount"));

		// 요청 메시지
		if (esbStatusCode.isEmpty()) {
			jmsTemplate.ifPresent(jms -> {
				loggingService.asyncStartLogging(jms, interfaceId, transactionId, sendSystemCode, receiveSystemCode,
						totalCount);
			});
			if (errorMessage != null) {
				jmsTemplate.ifPresent(jms -> {
					loggingService.asyncEndLogging(jms, interfaceId, transactionId, totalCount, ESBStatusConstants.FAIL,
							errorMessage);
				});
			}
			// 응답 메시지
		} else {
			jmsTemplate.ifPresent(jms -> {
				if (errorMessage != null) {
					loggingService.asyncEndLogging(jms, interfaceId, transactionId, totalCount, ESBStatusConstants.FAIL,
							errorMessage);
				} else {
					loggingService.asyncEndLogging(jms, interfaceId, transactionId, errorCount, esbStatusCode,
							esbStatusMessage);
				}
			});
		}
	}
}
