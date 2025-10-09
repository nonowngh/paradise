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
import mb.fw.paradise.constants.ESBAPIHeaderConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
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
		return gatewayWebClient.post().uri(targetPath).headers(headers -> {
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set(ESBAPIHeaderConstants.INTERFACE_ID, request.getInterfaceId());
			headers.set(ESBAPIHeaderConstants.TRANSACTION_ID, request.getTransactionId());
			headers.set(ESBAPIHeaderConstants.SEND_SYSTEM_CODE, sendSystemCode);
			headers.set(ESBAPIHeaderConstants.RECEIVE_SYSTEM_CODE, receiveSystemCode);
			headers.set(ESBAPIHeaderConstants.TOTAL_COUNT, String.valueOf(request.getTotalDataCount()));
			headers.set(ESBAPIHeaderConstants.CALL_BACK_PATH, callBackPath);
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
		return gatewayWebClient.post().uri(callbackPath).headers(headers -> {
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set(ESBAPIHeaderConstants.INTERFACE_ID, response.getInterfaceId());
			headers.set(ESBAPIHeaderConstants.TRANSACTION_ID, response.getTransactionId());
			headers.set(ESBAPIHeaderConstants.ESB_STATUS_CODE, response.getStatusCode());
			headers.set(ESBAPIHeaderConstants.ESB_STATUS_MESSAGE, response.getStatusMessage());
			headers.set(ESBAPIHeaderConstants.TOTAL_COUNT, String.valueOf(response.getTotalDataCount()));
			headers.set(ESBAPIHeaderConstants.ERROR_COUNT, String.valueOf(response.getErrorDataCount()));
//			headers.set("Authorization", "Bearer " + )); // 토큰이 있는 경우
		}).bodyValue(response).retrieve().bodyToMono(String.class).doOnError(error -> {
			log.error("결과 전송 중 오류 발생 : " + error.getMessage());
		}).doOnTerminate(() -> {
			log.info("결과 전송 완료");
		});
	}

}
