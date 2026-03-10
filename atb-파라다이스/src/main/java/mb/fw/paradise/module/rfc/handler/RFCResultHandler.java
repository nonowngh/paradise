package mb.fw.paradise.module.rfc.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.common.dto.APIResponseMessage;
import mb.fw.paradise.common.service.APIService;
import mb.fw.paradise.module.rfc.service.RFCModuleService;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RFCResultHandler {

	@Autowired(required = false)
	RFCModuleService rfcModuleService;

	private final APIService apiService;

	public RFCResultHandler(APIService apiService) {
		this.apiService = apiService;
	}

	public Mono<ServerResponse> rfcResultProcess(ServerRequest serverRequest) {
		return serverRequest.bodyToMono(APIResponseMessage.class)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("요청 body가 존재하지 않습니다."))) // body 없을 때 에러 처리
				.flatMap(response -> apiService.getInterfaceInfo(response.getInterfaceId())
						.flatMap(interfaceInfo -> rfcModuleService.rfcResult(response, interfaceInfo) // RFC 처리
								.map(count -> response)))
				.onErrorMap(error -> {
					log.error("Error [rfcResultProcess] -> {}", error.getMessage(), error); // 에러 처리
					return new RuntimeException(error.getMessage(), error);
				}).doOnSuccess(response -> log.info("데이터 결과 수신 완료[{}], 상태 코드 : {}", response.getTransactionId(),
						response.getStatusCode()))
				.then(ServerResponse.ok().bodyValue("[rfcResultProcess] 요청 수신 완료."));
	}
}
