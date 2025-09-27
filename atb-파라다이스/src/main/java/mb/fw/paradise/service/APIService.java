package mb.fw.paradise.service;

import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.constants.PatternType;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import reactor.core.publisher.Mono;

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
				.retrieve().bodyToMono(InterfaceInfo.class).switchIfEmpty(
						Mono.error(new NoSuchElementException("InterfaceInfo not found for id : " + interfaceId)));
	}

	public Mono<APIResponseMessage> callGateway(APIRequestMessage request, PatternType patternType,
			String targetSystemCode) {
		return gatewayWebClient.post().uri(patternType.getTargetContextPath()).bodyValue(request)
				.retrieve()
				.onStatus(HttpStatus::isError,
						clientResponse -> clientResponse.bodyToMono(String.class)
								.flatMap(errorBody -> Mono.error(new RuntimeException("Error: " + errorBody))))
				.bodyToMono(APIResponseMessage.class);
	}

	public Mono<String> callGatewayForResult(APIResponseMessage response, String callbackPath) {
		return gatewayWebClient.post().uri(callbackPath).bodyValue(response).retrieve().bodyToMono(String.class)
				.doOnError(error -> {
					// 오류 처리 (예: 로그 기록)
					log.error("결과 전송 중 오류 발생 : " + error.getMessage());
				}).doOnTerminate(() -> {
					// 요청이 종료된 후 실행할 작업 (예: 로깅, 트래킹 등)
					log.info("결과 전송 완료");
				});
	}

}
