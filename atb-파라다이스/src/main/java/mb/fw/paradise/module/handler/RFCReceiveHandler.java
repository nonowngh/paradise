package mb.fw.paradise.module.handler;

import java.util.List;

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
import mb.fw.paradise.module.service.RFCModuleService;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.service.ExceptionService;
import mb.fw.paradise.util.DataItemUtil;
import mb.fw.paradise.util.HttpHeaderUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RFCReceiveHandler {

	@Autowired(required = false)
	RFCModuleService rfcModuleService;

	private final APIService apiService;
	private final ExceptionService exceptionService;

	public RFCReceiveHandler(APIService apiService, ExceptionService exceptionService) {
		this.apiService = apiService;
		this.exceptionService = exceptionService;
	}

	public Mono<ServerResponse> rfcSyncProcess(ServerRequest serverRequest) {
		HttpHeaders requestHeader = serverRequest.headers().asHttpHeaders();
		return serverRequest.bodyToMono(APIRequestMessage.class)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("요청 body가 존재하지 않습니다."))) // body 없을 때 에러 처리
				.flatMap(request -> apiService.getInterfaceInfo(request.getInterfaceId()).flatMap(
						interfaceInfo -> rfcModuleService.rfcCallForReceive(interfaceInfo, request).flatMap(result -> {
							return HttpHeaderUtil.makeDefaultOkResponseHeader(requestHeader).bodyValue(result);
						})))
				.onErrorResume(error -> {
					log.error("Error [rfc-sync-process] -> {}", error.getMessage(), error);
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

	public Mono<ServerResponse> rfcProcess(ServerRequest serverRequest) {
		String callBackPath = HttpHeaderUtil.getHeader(serverRequest.headers().asHttpHeaders(),
				ESBApiHeaderConstants.CALL_BACK_PATH);
		String interfaceId = HttpHeaderUtil.getHeader(serverRequest.headers().asHttpHeaders(),
				ESBApiHeaderConstants.INTERFACE_ID);
		String transactionId = HttpHeaderUtil.getHeader(serverRequest.headers().asHttpHeaders(),
				ESBApiHeaderConstants.TRANSACTION_ID);
		int dataCount = HttpHeaderUtil.getIntHeader(serverRequest.headers().asHttpHeaders(),
				ESBApiHeaderConstants.DATA_COUNT);
		Mono<ServerResponse> immediateResponse = ServerResponse.ok().bodyValue("[rfcProcess] 요청 수신 완료.");

		List<String> contentTypes = serverRequest.headers().header(HttpHeaders.CONTENT_TYPE);
		boolean isNdjson = contentTypes.stream().anyMatch(t -> t.toLowerCase().contains("ndjson"));
		// 대량 데이터 처리
		Flux<APIRequestMessage> requestFlux;

		if (isNdjson) {
			log.info("Large data process..");
			requestFlux = DataItemUtil.parseNdjsonGzip(serverRequest);
		} else {
			requestFlux = serverRequest.bodyToMono(APIRequestMessage.class)
					.switchIfEmpty(Mono.error(new IllegalArgumentException("요청 body가 존재하지 않습니다."))).flux();
		}

		requestFlux.flatMap(request -> processRequest(request, callBackPath)).onErrorResume(error -> {
			log.error("[rfcProcess] 처리 중 오류 발생: transactionId={}, 오류 메시지 : {}", transactionId, error.getMessage(),
					error);
			return exceptionService
					.receiveHandlerExceptionProcess(error, interfaceId, transactionId, dataCount, callBackPath)
					.then(Mono.empty());
		}).subscribe(); // 별도 쓰레드에서 비동기 실행

		return immediateResponse;
	}

	private Mono<Void> processRequest(APIRequestMessage request, String callBackPath) {
		return apiService.getInterfaceInfo(request.getInterfaceId())
				.flatMap(interfaceInfo -> rfcModuleService.rfcCallForReceive(interfaceInfo, request)
						.flatMap(result -> apiService.callGatewayForResult(result, callBackPath)))
				.doOnSuccess(result -> log.info("[rfcProcess] 처리 완료: transactionId={}", request.getTransactionId()))
				.then(); // Mono<Void>
	}
}
