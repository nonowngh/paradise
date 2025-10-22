package mb.fw.paradise.gateway.filter;

import java.util.Optional;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
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
public class GatewayResponseLoggingFilter implements GlobalFilter {

	private final Optional<JmsTemplate> jmsTemplate;
	private LoggingService loggingService;

	public GatewayResponseLoggingFilter(Optional<JmsTemplate> jmsTemplate, LoggingService loggingService) {
		this.jmsTemplate = jmsTemplate;
		this.loggingService = loggingService;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpResponse originalResponse = exchange.getResponse();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                HttpHeaders backendHeaders = getDelegate().getHeaders();

                log.info("[HTTP 응답 헤더] : {}", backendHeaders);
                
        		String esbStatusCode = HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.ESB_STATUS_CODE);
        		String esbStatusMessage = HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
        		
    			HttpStatus statusCode = originalResponse.getStatusCode();
    			log.info("[GatewayFilter] Response Status Code: {}", statusCode);

    			if (statusCode != null && !statusCode.is2xxSuccessful()) {
    				log.warn("[gateway-logging-filter] Non-200 Response Detected: {}", statusCode);
    				String errorMessage = "요청 처리 오류 : [" + statusCode + "] "
    						+ exchange.getAttribute("gateway_exception_message");
    				if (ESBStatusConstants.FAIL.equals(esbStatusCode) && !esbStatusMessage.isEmpty()) {
    					log.info("ESB status message -> {}", esbStatusMessage);
    					errorMessage = "요청 처리 오류 : [" + statusCode + "] " + esbStatusMessage;
    				}
    				processByHeaders(backendHeaders, errorMessage, false);
    			} else {
    				processByHeaders(backendHeaders, null, false);
    			}
                
                return super.writeWith(body);
            }
        };
        
        decoratedResponse.beforeCommit(() -> {
            HttpHeaders headers = decoratedResponse.getHeaders();
            HttpStatus status = decoratedResponse.getStatusCode();
            log.info("응답 커밋 직전, 상태: {}, 헤더: {}", status, headers);
            return Mono.empty();
        });

        ServerWebExchange mutatedExchange = exchange.mutate().response(decoratedResponse).build();
		return chain.filter(mutatedExchange).doOnError(e -> {
		    log.error("응답 처리 중 오류 발생: {}", e.getMessage(), e);
		});
	}

	private void processByHeaders(HttpHeaders backendHeaders, String errorMessage, boolean isSyncRequest) {
		 String interfaceId = HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.INTERFACE_ID);
 		String transactionId = HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.TRANSACTION_ID);
 		String sendSystemCode = HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.SEND_SYSTEM_CODE);
 		String receiveSystemCode = HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE);
 		String esbStatusCode = HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.ESB_STATUS_CODE);
 		String esbStatusMessage = HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
 		int dataCount = HttpHeaderUtil.getIntHeader(backendHeaders, ESBApiHeaderConstants.DATA_COUNT) == 0 ? 1
 				: HttpHeaderUtil.getIntHeader(backendHeaders, ESBApiHeaderConstants.DATA_COUNT);
 		ApiMessageType apiMessagType = ApiMessageType
 				.valueOf(HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.API_MESSAGE_TYPE));

		// 요청 메시지
		if (apiMessagType.equals(ApiMessageType.REQUEST)) {
			jmsTemplate.ifPresent(jms -> {
				loggingService.asyncStartLogging(jms, interfaceId, transactionId, sendSystemCode, receiveSystemCode,
						dataCount);
			});
			if (errorMessage != null) {
				jmsTemplate.ifPresent(jms -> {
					loggingService.asyncEndLogging(jms, interfaceId, transactionId, dataCount, ESBStatusConstants.FAIL,
							errorMessage);
				});
			}
			// 응답 메시지
		} else if (apiMessagType.equals(ApiMessageType.RESPONSE)) {
			jmsTemplate.ifPresent(jms -> {
				if (errorMessage != null) {
					loggingService.asyncEndLogging(jms, interfaceId, transactionId, dataCount, ESBStatusConstants.FAIL,
							errorMessage);
				} else {
					loggingService.asyncEndLogging(jms, interfaceId, transactionId, dataCount, esbStatusCode,
							esbStatusMessage);
				}
			});
			// 동기 메시지
		} else {
			if (isSyncRequest) {
				jmsTemplate.ifPresent(jms -> {
					loggingService.asyncStartLogging(jms, interfaceId, transactionId, sendSystemCode, receiveSystemCode,
							dataCount);
				});
			} else {
				jmsTemplate.ifPresent(jms -> {
					if (errorMessage != null) {
						loggingService.asyncEndLogging(jms, interfaceId, transactionId, dataCount,
								ESBStatusConstants.FAIL, errorMessage);
					} else {
						loggingService.asyncEndLogging(jms, interfaceId, transactionId, dataCount, esbStatusCode,
								esbStatusMessage);
					}
				});
			}
		}
	}
}
