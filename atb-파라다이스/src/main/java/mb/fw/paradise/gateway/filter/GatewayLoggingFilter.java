package mb.fw.paradise.gateway.filter;

import java.util.Optional;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ApiMessageType;
import mb.fw.paradise.constants.ESBApiHeaderConstants;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.service.LoggingService;
import mb.fw.paradise.util.HttpHeaderUtil;
import reactor.core.publisher.Mono;

@Slf4j
//@Component
@Order(-2)
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
//		headers.forEach((key, valueList) -> {
//			String joinedValues = String.join(",", valueList);
//		});
		log.info("http request header info -> {}", headers.toString());

		// 필요한 헤더 추출
		String interfaceId = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.INTERFACE_ID);
		String transactionId = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.TRANSACTION_ID);
		String sendSystemCode = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.SEND_SYSTEM_CODE);
		String receiveSystemCode = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE);
		String esbStatusCode = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.ESB_STATUS_CODE);
		String esbStatusMessage = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
		int dataCount = HttpHeaderUtil.getIntHeader(headers, ESBApiHeaderConstants.DATA_COUNT) == 0 ? 1
				: HttpHeaderUtil.getIntHeader(headers, ESBApiHeaderConstants.DATA_COUNT);
		String callBackPath = HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.CALL_BACK_PATH);
		ApiMessageType apiMessagType = ApiMessageType
				.valueOf(HttpHeaderUtil.getHeader(headers, ESBApiHeaderConstants.API_MESSAGE_TYPE));

		// 후처리에 필요한 정보 저장
		exchange.getAttributes().put("interfaceId", interfaceId);
		exchange.getAttributes().put("transactionId", transactionId);
		exchange.getAttributes().put("sendSystemCode", sendSystemCode);
		exchange.getAttributes().put("receiveSystemCode", receiveSystemCode);
		exchange.getAttributes().put("esbStatusCode", esbStatusCode);
		exchange.getAttributes().put("esbStatusMessage", esbStatusMessage);
		exchange.getAttributes().put("dataCount", dataCount);
		exchange.getAttributes().put("callBackPath", callBackPath);
		exchange.getAttributes().put("apiMessagType", apiMessagType);

		// 동기 인터페이스 경우, 요청시에도 로깅 jms 송신
		if (apiMessagType.equals(ApiMessageType.SYNC)) {
			processByHeaders(exchange, null, true);
		}
		
		ServerHttpResponse originalResponse = exchange.getResponse();

		ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {

		    @Override
		    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		        HttpHeaders backendHeaders = getDelegate().getHeaders();

		        // ✅ 응답 헤더는 여기서 꺼내야 실제로 존재함
		        backendHeaders.forEach((key, values) -> {
		            for (String value : values) {
		                log.error("응답 헤더: {} = {}", key, value);

		                // 클라이언트로 전달하고 싶으면 명시적으로 복사
		                if (!originalResponse.getHeaders().containsKey(key)) {
		                    originalResponse.getHeaders().add(key, value);
		                }
		            }
		        });

		        return super.writeWith(body);
		    }
		};
		
        ServerWebExchange mutatedExchange = exchange.mutate().response(decoratedResponse).build();


		return chain.filter(mutatedExchange).doOnError(throwable -> {
			mutatedExchange.getAttributes().put("gateway_exception_message", throwable.getCause().getMessage());
		}).doFinally(signalType -> {
			HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
			log.info("http response header info -> {}", responseHeaders.toString());
			HttpStatus statusCode = exchange.getResponse().getStatusCode();
			log.info("[GatewayFilter] Response Status Code: {}", statusCode);

			String resultStatusCode = HttpHeaderUtil.getHeader(responseHeaders, ESBApiHeaderConstants.ESB_STATUS_CODE);
			String resultStatusMessage = HttpHeaderUtil.getHeader(responseHeaders, ESBApiHeaderConstants.ESB_STATUS_MESSAGE);
			if (statusCode != null && !statusCode.is2xxSuccessful()) {
				log.warn("[gateway-logging-filter] Non-200 Response Detected: {}", statusCode);
				String errorMessage = "요청 처리 오류 : [" + statusCode + "] "
						+ exchange.getAttribute("gateway_exception_message");
				if (ESBStatusConstants.FAIL.equals(resultStatusCode) && !resultStatusMessage.isEmpty()) {
					log.info("ESB status message -> {}", resultStatusMessage);
					errorMessage = "요청 처리 오류 : [" + statusCode + "] " + resultStatusMessage;
				}
				processByHeaders(exchange, errorMessage, false);
			} else {
				processByHeaders(exchange, null, false);
			}
		});

	}

	private void processByHeaders(ServerWebExchange exchange, String errorMessage, boolean isSyncRequest) {
		String interfaceId = (String) exchange.getAttribute("interfaceId");
		String transactionId = (String) exchange.getAttribute("transactionId");
		String sendSystemCode = (String) exchange.getAttribute("sendSystemCode");
		String receiveSystemCode = (String) exchange.getAttribute("receiveSystemCode");
		String esbStatusCode = (String) exchange.getAttribute("esbStatusCode");
		String esbStatusMessage = (String) exchange.getAttribute("esbStatusMessage");
		int dataCount = exchange.getAttribute("dataCount");
		ApiMessageType apiMessagType = exchange.getAttribute("apiMessagType");

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
