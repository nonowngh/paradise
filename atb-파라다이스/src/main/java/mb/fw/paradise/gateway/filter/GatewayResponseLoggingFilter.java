package mb.fw.paradise.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
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
import reactor.core.publisher.Flux;
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
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;

                    return super.writeWith(
                        fluxBody.map(dataBuffer -> {
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            DataBufferUtils.release(dataBuffer);

                            String responseBody = new String(content, StandardCharsets.UTF_8);
                            log.error("서버 응답 바디: {}", responseBody); // ← 여기서 에러 메시지 확인 가능

                            return bufferFactory().wrap(content);
                        })
                    );
                }
                return super.writeWith(body);
            }
        };
//        	@Override
//            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
//                HttpHeaders backendHeaders = getDelegate().getHeaders();
//
//                log.info("[HTTP 응답 헤더] : {}", backendHeaders);
//                
//        		String esbStatusCode = HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.ESB_STATUS_CODE);
//        		String esbStatusMessage = HttpHeaderUtil.getHeader(backendHeaders, ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
//        		
//    			HttpStatus statusCode = originalResponse.getStatusCode();
//    			log.info("[GatewayFilter] Response Status Code: {}", statusCode);
//
//    			if (statusCode != null && !statusCode.is2xxSuccessful()) {
//    				log.warn("[gateway-logging-filter] Non-200 Response Detected: {}", statusCode);
//    				String errorMessage = "요청 처리 오류 : [" + statusCode + "] "
//    						+ exchange.getAttribute("gateway_exception_message");
//    				if (ESBStatusConstants.FAIL.equals(esbStatusCode) && !esbStatusMessage.isEmpty()) {
//    					log.info("ESB status message -> {}", esbStatusMessage);
//    					errorMessage = "요청 처리 오류 : [" + statusCode + "] " + esbStatusMessage;
//    				}
//    				processByHeaders(backendHeaders, errorMessage, false);
//    			} else {
//    				processByHeaders(backendHeaders, null, false);
//    			}
//                
//                return super.writeWith(body);
//            }
//        };
        ServerWebExchange mutatedExchange = exchange.mutate().response(decoratedResponse).build();
        
        String interfaceId = (String) mutatedExchange.getAttribute("interfaceId");
        String transactionId = (String) mutatedExchange.getAttribute("transactionId");
        String sendSystemCode = (String) mutatedExchange.getAttribute("sendSystemCode");
        String receiveSystemCode = (String) mutatedExchange.getAttribute("receiveSystemCode");
        int dataCount = mutatedExchange.getAttribute("dataCount");
        
        decoratedResponse.beforeCommit(() -> {
            HttpHeaders headers = decoratedResponse.getHeaders();
            HttpStatus status = decoratedResponse.getStatusCode();
            headers.add(ESBApiHeaderConstants.INTERFACE_ID, interfaceId);
            headers.add(ESBApiHeaderConstants.TRANSACTION_ID, transactionId);
            headers.add(ESBApiHeaderConstants.SEND_SYSTEM_CODE, sendSystemCode);
            headers.add(ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE, receiveSystemCode);
            if(!status.is2xxSuccessful()) {
                headers.add(ESBApiHeaderConstants.ESB_STATUS_CODE, ESBStatusConstants.FAIL);
                headers.add(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, sendSystemCode);

            }
            log.info("응답 커밋 직전, 상태: {}, 헤더: {}", status, headers);
            return Mono.empty();
        });

        
		return chain.filter(mutatedExchange).doOnError(e -> {
		    log.error("응답 처리 중 오류 발생: {}", e.getMessage(), e);
		    decoratedResponse.getHeaders().add(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, e.getMessage());
		});
//				.doFinally(onFinally);
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
