package mb.fw.paradise.gateway.filter;

import java.util.Optional;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ApiMessageType;
import mb.fw.paradise.constants.ESBApiHeaderConstants;
import mb.fw.paradise.service.LoggingService;
import mb.fw.paradise.util.HttpHeaderUtil;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(100)
public class GatewayRequestLoggingFilter implements GlobalFilter {

	private final Optional<JmsTemplate> jmsTemplate;
	private LoggingService loggingService;

	public GatewayRequestLoggingFilter(Optional<JmsTemplate> jmsTemplate, LoggingService loggingService) {
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
        log.info("[HTTP 요청 헤더] : {}", headers);

		// 필요한 헤더 추출
		String interfaceId = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.INTERFACE_ID);
		String transactionId = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.TRANSACTION_ID);
		String sendSystemCode = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.SEND_SYSTEM_CODE);
		String receiveSystemCode = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE);
		int dataCount = HttpHeaderUtil.getIntHeader(headers, ESBApiHeaderConstants.DATA_COUNT) == 0 ? 1
				: HttpHeaderUtil.getIntHeader(headers, ESBApiHeaderConstants.DATA_COUNT);
		ApiMessageType apiMessagType = ApiMessageType
				.valueOf(HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.API_MESSAGE_TYPE));

		// 후처리에 필요한 정보 저장
		exchange.getAttributes().put("interfaceId", interfaceId);
		exchange.getAttributes().put("transactionId", transactionId);
		exchange.getAttributes().put("sendSystemCode", sendSystemCode);
		exchange.getAttributes().put("receiveSystemCode", receiveSystemCode);
		exchange.getAttributes().put("dataCount", dataCount);

		// 동기 인터페이스 경우, 요청시에도 로깅 jms 송신
		if (apiMessagType.equals(ApiMessageType.SYNC)) {
			processByHeaders(exchange, null, true);
		}
		
		return chain.filter(exchange);
	}

	private void processByHeaders(ServerWebExchange exchange, String errorMessage, boolean isSyncRequest) {
		String interfaceId = (String) exchange.getAttribute("interfaceId");
		String transactionId = (String) exchange.getAttribute("transactionId");
		String sendSystemCode = (String) exchange.getAttribute("sendSystemCode");
		String receiveSystemCode = (String) exchange.getAttribute("receiveSystemCode");
		int dataCount = exchange.getAttribute("dataCount");

		jmsTemplate.ifPresent(jms -> {
			loggingService.asyncStartLogging(jms, interfaceId, transactionId, sendSystemCode, receiveSystemCode,
					dataCount);
		});		
	}
}
