package mb.fw.paradise.module.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.module.service.ReceiveDBModuleService;
import mb.fw.paradise.service.APIService;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DBProcessHandler {

	private final ReceiveDBModuleService receiveDBModuleService;
	private final APIService apiService;

	public DBProcessHandler(APIService apiService, ReceiveDBModuleService receiveDBModuleService) {
		this.receiveDBModuleService = receiveDBModuleService;
		this.apiService = apiService;
	}

	public Mono<ServerResponse> dbProcess(ServerRequest serverRequest) {
		APIRequestMessage request = (APIRequestMessage) serverRequest.attributes().get("cachedBody");

		if (request == null) {
			return ServerResponse.badRequest().bodyValue("요청 body가 존재하지 않습니다.");
		}

		return apiService.getInterfaceInfo(request.getInterfaceId()) // 인터페이스 정보 조회
				.flatMap(interfaceInfo -> receiveDBModuleService.dbProcessAndResponse(interfaceInfo, request) // DB 처리
						.doOnNext(result -> {
							apiService.callGatewayForResult(result, request.getCallBackPath()); // 결과 호출
						}))
				.onErrorMap(error -> {
					log.error("Error [dbProcess] -> {}", error.getMessage()); // 에러 처리
					return new RuntimeException(error.getMessage(), error);
				}).then(ServerResponse.ok().build());
	}

}
