package mb.fw.paradise.filter;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.service.LoggingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(1)
public class GatewayLoggingFilter implements GlobalFilter {

	private final Optional<JmsTemplate> jmsTemplate;
	private LoggingService loggingService;
	private final ObjectMapper objectMapper;

	public GatewayLoggingFilter(Optional<JmsTemplate> jmsTemplate, LoggingService loggingService,
			ObjectMapper objectMapper) {
		this.jmsTemplate = jmsTemplate;
		this.loggingService = loggingService;
		this.objectMapper = objectMapper;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// 요청 로깅
		log.info("[gateway-logging-filter] Request: " + exchange.getRequest().getMethod() + " "
				+ exchange.getRequest().getURI());

		ServerHttpRequest request = exchange.getRequest();

		if (MediaType.APPLICATION_JSON.isCompatibleWith(request.getHeaders().getContentType())) {
			return DataBufferUtils.join(request.getBody()).flatMap(dataBuffer -> {
				byte[] bytes = new byte[dataBuffer.readableByteCount()];
				dataBuffer.read(bytes);
				DataBufferUtils.release(dataBuffer); // 중요: 메모리 해제

				String bodyString = new String(bytes, StandardCharsets.UTF_8);
				log.debug("Request Body: {}", bodyString);

				// 복사한 body를 새로 넣어주기 위한 래핑
				Flux<DataBuffer> cachedBody = Flux.defer(() -> {
					DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
					return Mono.just(buffer);
				});
				ServerHttpRequest mutatedRequest = request.mutate().build();
				ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(mutatedRequest) {
					@Override
					public Flux<DataBuffer> getBody() {
						return cachedBody;
					}
				};
				exchange.getAttributes().put("cachedRequestBody", bodyString);
				// 체인 실행 + 응답 상태 처리
				return chain.filter(exchange.mutate().request(decoratedRequest).build()).doFinally(signalType -> {
					HttpStatus statusCode = exchange.getResponse().getStatusCode();
					log.info("[GatewayFilter] Response Status Code: {}", statusCode);

					String savedRequestBody = (String) exchange.getAttribute("cachedRequestBody");
					if (statusCode != null && !statusCode.is2xxSuccessful()) {
						log.warn("[gateway-logging-filter] Non-200 Response Detected: {}", statusCode);
						String errorMessage = "요청 처리 오류 : [" + statusCode + "]" + statusCode.getReasonPhrase();
						processByBodyType(savedRequestBody, errorMessage);
					} else
						processByBodyType(savedRequestBody, null);
				});
			});
		}
		return chain.filter(exchange); // JSON이 아닌 경우 그냥 pass
	}

	private void processByBodyType(String savedRequestBody, String errorMessage) {
		try {
			APIRequestMessage requestMessage = objectMapper.readValue(savedRequestBody, APIRequestMessage.class);
			if (requestMessage.getInterfaceId().isEmpty()) {
				if (errorMessage != null) {
					jmsTemplate.ifPresent(jms -> {
						loggingService.asyncEndLogging(jms,
								APIResponseMessage.builder().interfaceId(requestMessage.getInterfaceId())
										.transactionId(requestMessage.getTransactionId())
										.statusCode(ESBStatusConstants.FAIL).statusMessage(errorMessage)
										.errorDataCount(requestMessage.getSendDataCount()).build());
					});
				} else {
					jmsTemplate.ifPresent(jms -> {
						loggingService.asyncStartLogging(jms, requestMessage);
					});
				}
			}
		} catch (Exception e) {
			try {
				APIResponseMessage responseMessage = objectMapper.readValue(savedRequestBody, APIResponseMessage.class);
				if (responseMessage.getInterfaceId().isEmpty()) {
					if (errorMessage != null) {
						responseMessage.setStatusCode(ESBStatusConstants.FAIL);
						responseMessage.setStatusMessage(errorMessage);
						responseMessage.setErrorDataCount(1);
					}
					jmsTemplate.ifPresent(jms -> {
						loggingService.asyncEndLogging(jms, responseMessage);
					});
				}
			} catch (Exception e1) {
				log.error("Body type error(can't not convert json body -> pojo) body : {}", savedRequestBody);
			}
		}
	}
}
