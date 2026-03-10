package mb.fw.paradise.module.gateway.filter;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.slf4j.MDC; // log4j 대신 slf4j 권장
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.common.constants.ApiMessageType;
import mb.fw.paradise.common.constants.ESBApiHeaderConstants;
import mb.fw.paradise.common.constants.ESBStatusConstants;
import mb.fw.paradise.common.logging.InterfaceLogging;
import mb.fw.paradise.common.util.HttpHeaderUtil;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GatewayHeaderLoggingFilter implements GlobalFilter, Ordered {

	private final Optional<InterfaceLogging> interfaceLogging;

	public GatewayHeaderLoggingFilter(Optional<InterfaceLogging> interfaceLogging) {
		this.interfaceLogging = interfaceLogging;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		HttpHeaders headers = request.getHeaders();

		// 1. 필수 메타데이터 추출 및 컨텍스트 저장
		String txId = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.TRANSACTION_ID);
		String interfaceId = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.INTERFACE_ID);
		String msgTypeStr = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.API_MESSAGE_TYPE);

		// 비동기 스레드 전파를 위해 MDC 설정 (현재 스레드용)
		MDC.put("esbTxId", txId);

		// 로깅을 위한 exchange attributes 세팅
		populateExchangeAttributes(exchange, headers, txId, interfaceId, msgTypeStr);

		logRoutingInfo(exchange, msgTypeStr);

		// 2. 요청 시작 로깅 (Async)
		ApiMessageType apiMessageType = safeValueOf(msgTypeStr);
		if (apiMessageType == ApiMessageType.SYNC || apiMessageType == ApiMessageType.REQUEST) {
			processLogging(exchange.getAttributes(), true);
		}

		// 3. 응답 헤더 로깅 예약 (커밋 직전 실행)
		exchange.getResponse().beforeCommit(() -> {
			MDC.put("esbTxId", txId); // 커밋 스레드에 다시 할당
			log.info("HTTP Response Header: [{}] {}", exchange.getResponse().getStatusCode(),
					exchange.getResponse().getHeaders());
			return Mono.empty();
		});

		// 4. 필터 체인 실행 및 후처리
		return chain.filter(exchange).doOnSuccess(v -> handleSuccess(exchange, txId, apiMessageType))
				.doOnError(ex -> handleError(exchange, txId, ex)).doFinally(signal -> MDC.remove("esbTxId"));
	}

	private void populateExchangeAttributes(ServerWebExchange exchange, HttpHeaders headers, String txId,
			String interfaceId, String msgTypeStr) {
		Map<String, Object> attrs = exchange.getAttributes();
		attrs.put(ESBApiHeaderConstants.TRANSACTION_ID, txId);
		attrs.put(ESBApiHeaderConstants.INTERFACE_ID, interfaceId);
		attrs.put(ESBApiHeaderConstants.API_MESSAGE_TYPE, msgTypeStr);
		attrs.put(ESBApiHeaderConstants.SEND_SYSTEM_CODE,
				HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.SEND_SYSTEM_CODE));
		attrs.put(ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE,
				HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE));
		attrs.put(ESBApiHeaderConstants.DATA_COUNT,
				HttpHeaderUtil.getIntHeader(headers, ESBApiHeaderConstants.DATA_COUNT));

		String statusMsg = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
		attrs.put(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, decodeBase64Safe(statusMsg));
		attrs.put(ESBApiHeaderConstants.ESB_STATUS_CODE,
				HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.ESB_STATUS_CODE));
	}

	private void handleSuccess(ServerWebExchange exchange, String txId, ApiMessageType type) {
		MDC.put("esbTxId", txId);
		HttpHeaders resHeaders = exchange.getResponse().getHeaders();
		Map<String, Object> attrs = exchange.getAttributes();

		String encodedMsg = resHeaders.getFirst(ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
		attrs.put(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, decodeBase64Safe(encodedMsg));
		attrs.put(ESBApiHeaderConstants.ESB_STATUS_CODE,
				HttpHeaderUtil.getHeader(resHeaders, ESBApiHeaderConstants.ESB_STATUS_CODE));

		processLogging(attrs, false);
	}

	private void handleError(ServerWebExchange exchange, String txId, Throwable ex) {
		MDC.put("esbTxId", txId);
		log.error("Target Service Call Error: {}", ex.getMessage());

		Map<String, Object> attrs = exchange.getAttributes();
		attrs.put(ESBApiHeaderConstants.ESB_STATUS_CODE, ESBStatusConstants.FAIL);
		attrs.put(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, "GW_ERROR: " + ex.getMessage());

		processLogging(attrs, false);
	}

	private void processLogging(Map<String, Object> attrs, boolean isStart) {
		String interfaceId = (String) attrs.get(ESBApiHeaderConstants.INTERFACE_ID);
		String txId = (String) attrs.get(ESBApiHeaderConstants.TRANSACTION_ID);
		String sendSys = (String) attrs.get(ESBApiHeaderConstants.SEND_SYSTEM_CODE);
		String recvSys = (String) attrs.get(ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE);
		String statusCode = (String) attrs.get(ESBApiHeaderConstants.ESB_STATUS_CODE);
		String statusMsg = (String) attrs.get(ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
		int count = (Integer) attrs.getOrDefault(ESBApiHeaderConstants.DATA_COUNT, 1);
		ApiMessageType type = safeValueOf((String) attrs.get(ESBApiHeaderConstants.API_MESSAGE_TYPE));

		interfaceLogging.ifPresent(logging -> {
			if (isStart) {
				logging.asyncStartLogging(interfaceId, txId, sendSys, recvSys, count);
			} else {
				// 비동기 요청(REQUEST)이면서 실패가 아닌 경우(정상 전송)는 Start만 남기고 종료 로깅은 생략하거나 정책에 따름
				if (type == ApiMessageType.REQUEST
						&& (statusCode == null || !ESBStatusConstants.FAIL.equals(statusCode))) {
					return;
				}
				logging.asyncEndLogging(interfaceId, txId, count, statusCode, statusMsg);
			}
		});
	}

	private String decodeBase64Safe(String value) {
		if (value == null || value.isEmpty())
			return value;
		try {
			if (HttpHeaderUtil.isBase64(value)) {
				return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
			}
		} catch (Exception e) {
			log.warn("Base64 decoding failed for value: {}", value);
		}
		return value;
	}

	private void logRoutingInfo(ServerWebExchange exchange, String msgType) {
		URI originalUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
		URI requestUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
		log.info("Routing [{}] : '{}' ---> '{}'", msgType, originalUri, requestUri);
	}

	private ApiMessageType safeValueOf(String name) {
		try {
			return ApiMessageType.valueOf(name);
		} catch (Exception e) {
			return ApiMessageType.SYNC; // 기본값
		}
	}
}