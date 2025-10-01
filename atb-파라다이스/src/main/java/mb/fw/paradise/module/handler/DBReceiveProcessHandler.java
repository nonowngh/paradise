package mb.fw.paradise.module.handler;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ESBAPIHeaderConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.module.service.DBModuleService;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.util.HttpHeaderUtil;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@ConditionalOnBean(DataSource.class)
public class DBReceiveProcessHandler {

	private final DBModuleService dbModuleService;
	private final APIService apiService;

	public DBReceiveProcessHandler(APIService apiService, DBModuleService dbModuleService) {
		this.dbModuleService = dbModuleService;
		this.apiService = apiService;
	}

	public Mono<ServerResponse> dbProcess(ServerRequest serverRequest) {
		String callBackPath = HttpHeaderUtil.getHeader(serverRequest.headers().asHttpHeaders(),
				ESBAPIHeaderConstants.CALL_BACK_PATH);
		return serverRequest.bodyToMono(APIRequestMessage.class)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("요청 body가 존재하지 않습니다."))) // body 없을 때 에러 처리
				.flatMap(request -> apiService.getInterfaceInfo(request.getInterfaceId())
						.flatMap(interfaceInfo -> dbModuleService.dbProcessAndResponse(interfaceInfo, request)
								.doOnNext(result -> apiService.callGatewayForResult(result, callBackPath))))
				.onErrorMap(error -> {
					log.error("Error [dbProcess] -> {}", error.getMessage(), error); // 에러 처리
					return new RuntimeException(error.getMessage(), error);
				}).then(ServerResponse.ok().bodyValue("[dbProcess] 요청 수신 완료."));
	}
}
