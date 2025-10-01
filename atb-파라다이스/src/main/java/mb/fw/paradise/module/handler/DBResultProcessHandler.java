package mb.fw.paradise.module.handler;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ConditionalOnBean(DataSource.class)
public class DBResultProcessHandler {

	private final DBModuleService dbModuleService;
	private final APIService apiService;

	public DBResultProcessHandler(APIService apiService, DBModuleService dbModuleService) {
		this.dbModuleService = dbModuleService;
		this.apiService = apiService;
	}

	public Mono<ServerResponse> dbResultProcess(ServerRequest serverRequest) {
		return serverRequest.bodyToMono(APIResponseMessage.class)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("요청 body가 존재하지 않습니다."))) // body 없을 때 에러 처리
				.flatMap(response -> apiService.getInterfaceInfo(response.getInterfaceId())
						.flatMap(interfaceInfo -> dbModuleService.dbResult(response, interfaceInfo) // DB 처리
								.doOnNext(count -> log.info("[dbResultProcess] 업데이트 완료. 처리 건수: {}", count))))
				.onErrorMap(error -> {
					log.error("Error [dbResultProcess] -> {}", error.getMessage(), error); // 에러 처리
					return new RuntimeException(error.getMessage(), error);
				}).then(ServerResponse.ok().bodyValue("[dbResultProcess] 요청 수신 완료."));
	}

}
