package mb.fw.paradise.service;

import java.time.Duration;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.constants.ApiMessageType;
import mb.fw.paradise.constants.ESBApiHeaderConstants;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.service.exception.CustomGatewayException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Service
public class APIService {

	private final WebClient interfaceInfoWebClient;
	private final WebClient gatewayWebClient;

	public APIService(@Qualifier("interfaceInfoWebClient") WebClient interfaceInfoWebClient,
			@Qualifier("gatewayWebClient") WebClient gatewayWebClient) {
		this.interfaceInfoWebClient = interfaceInfoWebClient;
		this.gatewayWebClient = gatewayWebClient;
	}

	public Mono<InterfaceInfo> getInterfaceInfo(String interfaceId) {
		return interfaceInfoWebClient.get().uri(uriBuilder -> uriBuilder.queryParam("interfaceId", interfaceId).build())
				.retrieve().bodyToMono(InterfaceInfo.class).retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
				.switchIfEmpty(
						Mono.error(new NoSuchElementException("InterfaceInfo not found for id : " + interfaceId)));
	}

	public Mono<String> callGateway(APIRequestMessage request, String targetPath, String sendSystemCode,
			String receiveSystemCode, String callBackPath) {
		return gatewayWebClient.post().uri("/" + targetPath.toLowerCase()).headers(headers -> {
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set(ESBApiHeaderConstants.INTERFACE_ID, request.getInterfaceId());
			headers.set(ESBApiHeaderConstants.TRANSACTION_ID, request.getTransactionId());
			headers.set(ESBApiHeaderConstants.SEND_SYSTEM_CODE, sendSystemCode);
			headers.set(ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE, receiveSystemCode);
			headers.set(ESBApiHeaderConstants.DATA_COUNT, String.valueOf(request.getDataCount()));
			headers.set(ESBApiHeaderConstants.CALL_BACK_PATH, callBackPath);
			headers.set(ESBApiHeaderConstants.API_MESSAGE_TYPE, ApiMessageType.REQUEST.name());
//			headers.set("Authorization", "Bearer " + )); // 토큰이 있는 경우
		}).bodyValue(request).retrieve()
				.onStatus(HttpStatus::isError,
						clientResponse -> clientResponse.bodyToMono(String.class).flatMap(
								errorBody -> Mono.error(new RuntimeException("데이터 전송 중 오류 발생 : " + errorBody))))
				.bodyToMono(String.class).doOnTerminate(() -> {
					log.info("데이터 전송 완료");
				});
	}

	public Mono<String> callGatewayForResult(APIResponseMessage response, String callbackPath) {
		return gatewayWebClient.post().uri(callbackPath.toLowerCase()).headers(headers -> {
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set(ESBApiHeaderConstants.INTERFACE_ID, response.getInterfaceId());
			headers.set(ESBApiHeaderConstants.TRANSACTION_ID, response.getTransactionId());
			headers.set(ESBApiHeaderConstants.ESB_STATUS_CODE, response.getStatusCode());
			headers.set(ESBApiHeaderConstants.ESB_STATUS_MESSAGE, response.getStatusMessage());
			headers.set(ESBApiHeaderConstants.DATA_COUNT, String.valueOf(response.getDataCount()));
			headers.set(ESBApiHeaderConstants.API_MESSAGE_TYPE, ApiMessageType.RESPONSE.name());
//			headers.set("Authorization", "Bearer " + )); // 토큰이 있는 경우
		}).bodyValue(response).retrieve().bodyToMono(String.class).doOnError(error -> {
			log.error("결과 전송 중 오류 발생 : " + error.getMessage());
		}).doOnTerminate(() -> {
			log.info("결과 전송 완료");
		});
	}

	public Mono<APIResponseMessage> callGatewaySync(APIRequestMessage request, String targetPath, String sendSystemCode,
			String receiveSystemCode) {
		return gatewayWebClient.post().uri("/" + targetPath.toLowerCase()).headers(headers -> {
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set(ESBApiHeaderConstants.INTERFACE_ID, request.getInterfaceId());
			headers.set(ESBApiHeaderConstants.TRANSACTION_ID, request.getTransactionId());
			headers.set(ESBApiHeaderConstants.SEND_SYSTEM_CODE, sendSystemCode);
			headers.set(ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE, receiveSystemCode);
			headers.set(ESBApiHeaderConstants.DATA_COUNT,
					request.getDataCount() == 0 ? String.valueOf(1) : String.valueOf(request.getDataCount()));
			headers.set(ESBApiHeaderConstants.API_MESSAGE_TYPE, ApiMessageType.SYNC.name());
//			headers.set(ESBAPIHeaderConstants.CALL_BACK_PATH, callBackPath);
//			headers.set("Authorization", "Bearer " + )); // 토큰이 있는 경우
		}).bodyValue(request).retrieve()
				.onStatus(HttpStatus::isError, response -> response.bodyToMono(APIResponseMessage.class)
						.switchIfEmpty(Mono.error(new CustomGatewayException("응답 바디가 없음",
								APIResponseMessage.builder().interfaceId(request.getInterfaceId())
										.transactionId(request.getTransactionId()).dataCount(request.getDataCount())
										.statusCode(ESBStatusConstants.FAIL).statusMessage("응답 바디 null").build())))
						.flatMap(errorBody -> Mono.error(new CustomGatewayException(
								"게이트웨이 전송 중 오류 발생(APIResponseMessage) : " + errorBody.getStatusMessage(), errorBody))))
				.bodyToMono(APIResponseMessage.class)
				.doOnSuccess(response -> log.info("데이터 전송 완료 - 응답 바디 : {}", response));
	}
//												.transactionId(request.getTransactionId()).dataCount(request.getDataCount())
//												.statusCode(ESBStatusConstants.FAIL).statusMessage(body).build()))))))
//				.flatMap(errorBody -> Mono.error(new CustomGatewayException(
//						"게이트웨이 전송 중 오류 발생(APIResponseMessage) : " + errorBody.getStatusMessage(), errorBody).getApiResponse()))
//				.onErrorResume(e -> response.bodyToMono(String.class).doOnNext(error -> log.error("Error -> {}", error))
//						.flatMap(body -> Mono.error(new CustomGatewayException("게이트웨이 전송 중 오류 발생(String) : " + body,
//								APIResponseMessage.builder().interfaceId(request.getInterfaceId())
//										.transactionId(request.getTransactionId()).dataCount(request.getDataCount())
//										.statusCode(ESBStatusConstants.FAIL).statusMessage(body).build())).getApiResponse()))

//	public Mono<APIResponseMessage> callGatewaySync(APIRequestMessage request, String targetPath, String sendSystemCode,
//			String receiveSystemCode) {
//
//		return gatewayWebClient.post().uri("/" + targetPath.toLowerCase()).headers(headers -> {
//			headers.setContentType(MediaType.APPLICATION_JSON);
//			headers.set(ESBApiHeaderConstants.INTERFACE_ID, request.getInterfaceId());
//			headers.set(ESBApiHeaderConstants.TRANSACTION_ID, request.getTransactionId());
//			headers.set(ESBApiHeaderConstants.SEND_SYSTEM_CODE, sendSystemCode);
//			headers.set(ESBApiHeaderConstants.RECEIVE_SYSTEM_CODE, receiveSystemCode);
//			headers.set(ESBApiHeaderConstants.DATA_COUNT,
//					request.getDataCount() == 0 ? "1" : String.valueOf(request.getDataCount()));
//			headers.set(ESBApiHeaderConstants.API_MESSAGE_TYPE, ApiMessageType.SYNC.name());
//		}).bodyValue(request).exchangeToMono(response -> {
//			HttpHeaders responseHeaders = response.headers().asHttpHeaders();
//			responseHeaders.forEach((key, values) -> {
//				values.forEach(value -> log.info("응답 헤더: {} = {}", key, value));
//			});
//			if (response.statusCode().isError()) {
//				response.bodyToMono(APIResponseMessage.class)
//						.map(errorBody -> (Throwable) new CustomGatewayException(
//								"게이트웨이 전송 중 오류 발생(APIResponseMessage) : " + errorBody.getStatusMessage(), errorBody))
//						.onErrorResume(e -> response.bodyToMono(String.class).map(
//								body -> (Throwable) new CustomGatewayException("게이트웨이 전송 중 오류 발생(String) : " + body,
//										APIResponseMessage.builder().interfaceId(request.getInterfaceId())
//												.transactionId(request.getTransactionId())
//												.dataCount(request.getDataCount()).statusCode(ESBStatusConstants.FAIL)
//												.statusMessage(body).build())));
//			}
//			return response.bodyToMono(APIResponseMessage.class);
//		}).doOnSuccess(response -> log.info("데이터 전송 완료"));
//	}

}
