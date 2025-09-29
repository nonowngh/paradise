package mb.fw.paradise.module.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.module.service.DBModuleService;
import mb.fw.paradise.service.APIService;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DBResultProcessHandler {

	private final DBModuleService dbModuleService;
	private final APIService apiService;

	public DBResultProcessHandler(APIService apiService, DBModuleService dbModuleService) {
		this.dbModuleService = dbModuleService;
		this.apiService = apiService;
	}

	public Mono<ServerResponse> dbResultProcess(ServerRequest serverRequest) {
		APIResponseMessage response = (APIResponseMessage) serverRequest.attributes().get("cachedBody");

		if (response == null) {
			return ServerResponse.badRequest().bodyValue("요청 body가 존재하지 않습니다.");
		}
		return apiService.getInterfaceInfo(response.getInterfaceId()) // 인터페이스 정보 조회
				.flatMap(interfaceInfo -> dbModuleService.dbResult(response, interfaceInfo) // DB 처리
						.doOnNext(count -> log.info("Result updated rows: {}", count))
						.flatMap(count -> ServerResponse.ok().bodyValue("[dbResultProcess] 업데이트 완료. 처리 건수: " + count)))
				.onErrorMap(error -> {
					log.error("Error [dbResultProcess] -> {}", error.getMessage(), error); // 에러 처리
					return new RuntimeException(error.getMessage(), error);
				});
	}

}
