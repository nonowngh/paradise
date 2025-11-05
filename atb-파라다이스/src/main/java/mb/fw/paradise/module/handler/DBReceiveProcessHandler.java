package mb.fw.paradise.module.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ESBApiHeaderConstants;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.module.service.DBModuleService;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.service.ExceptionService;
import mb.fw.paradise.util.HttpHeaderUtil;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DBReceiveProcessHandler {

	@Autowired(required = false)
	DBModuleService dbModuleService;

	private final APIService apiService;
	private final ExceptionService exceptionService;

	public DBReceiveProcessHandler(APIService apiService, ExceptionService exceptionService) {
		this.apiService = apiService;
		this.exceptionService = exceptionService;
	}

	public Mono<ServerResponse> dbProcess(ServerRequest serverRequest) {
		String callBackPath = HttpHeaderUtil.getHeader(serverRequest.headers().asHttpHeaders(),
				ESBApiHeaderConstants.CALL_BACK_PATH);
		String interfaceId = HttpHeaderUtil.getHeaderIgnoreCase(serverRequest.headers().asHttpHeaders(),
				ESBApiHeaderConstants.INTERFACE_ID);
		String transactionId = HttpHeaderUtil.getHeaderIgnoreCase(serverRequest.headers().asHttpHeaders(),
				ESBApiHeaderConstants.TRANSACTION_ID);
		int dataCount = HttpHeaderUtil.getIntHeaderIgnoreCase(serverRequest.headers().asHttpHeaders(),
				ESBApiHeaderConstants.DATA_COUNT);
		Mono<ServerResponse> immediateResponse = ServerResponse.ok().bodyValue("[dbProcess] 요청 수신 완료.");
		serverRequest.bodyToMono(APIRequestMessage.class)
				// body 없으면 예외 발생 → RouterFunction 예외 핸들러로 전달
				.switchIfEmpty(Mono.error(new IllegalArgumentException("요청 body가 존재하지 않습니다."))).flatMap(request -> {
					// 내부 비동기 처리
					apiService.getInterfaceInfo(request.getInterfaceId())
							.flatMap(interfaceInfo -> dbModuleService.dbProcessAndResponse(interfaceInfo, request)
									.flatMap(result -> apiService.callGatewayForResult(result, callBackPath)))
							.doOnSuccess(result -> log.info("[dbProcess] 비동기 처리 완료")).onErrorResume(error -> {
								log.error("[dbProcess] 비동기 처리 중 예외 발생: {}", error.getMessage(), error);
								// 예외 처리 Mono를 체인에 포함 → onErrorDropped 방지
								return exceptionService.receiveHandlerExceptionProcess(error, interfaceId,
										transactionId, dataCount, callBackPath).onErrorResume(err -> {
											log.error("[dbProcess] 예외 처리 전송 실패", err);
											return Mono.empty(); // 에러 무시
										}).then(Mono.empty());
							}).subscribe();
					return Mono.empty();
				}).subscribe(); // 비동기 처리
		return immediateResponse;
	}

	public Mono<ServerResponse> dbSyncProcess(ServerRequest serverRequest) {
		HttpHeaders requestHeader = serverRequest.headers().asHttpHeaders();
		return serverRequest.bodyToMono(APIRequestMessage.class)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("요청 body가 존재하지 않습니다."))) // body 없을 때 에러 처리
				.flatMap(request -> apiService.getInterfaceInfo(request.getInterfaceId())
						.flatMap(interfaceInfo -> dbModuleService.dbProcessAndResponse(interfaceInfo, request)
								.flatMap(result -> {
									return HttpHeaderUtil.makeDefaultOkResponseHeader(requestHeader).bodyValue(result);
								})))
				.onErrorResume(error -> {
					log.error("Error [db-sync-process] -> {}", error.getMessage(), error);
					return HttpHeaderUtil.makeDefaultErrorResponseHeader(requestHeader, error.getMessage())
							.bodyValue(APIResponseMessage.builder()
									.interfaceId(
											HttpHeaderUtil.getHeader(requestHeader, ESBApiHeaderConstants.INTERFACE_ID))
									.transactionId(HttpHeaderUtil.getHeader(requestHeader,
											ESBApiHeaderConstants.TRANSACTION_ID))
									.dataCount(HttpHeaderUtil.getIntHeader(requestHeader,
											ESBApiHeaderConstants.DATA_COUNT))
									.statusCode(ESBStatusConstants.FAIL).statusMessage(error.getMessage()).build());
				});
	}
}
