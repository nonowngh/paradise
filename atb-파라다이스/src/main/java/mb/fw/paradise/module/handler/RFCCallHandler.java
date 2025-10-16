package mb.fw.paradise.module.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ESBAPIHeaderConstants;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.module.service.RFCModuleService;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.util.HttpHeaderUtil;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RFCCallHandler {

	@Autowired(required = false)
	RFCModuleService rfcModuleService;

	private final APIService apiService;

	public RFCCallHandler(APIService apiService) {
		this.apiService = apiService;
	}

	public Mono<ServerResponse> rfcSyncProcess(ServerRequest serverRequest) {
		HttpHeaders requestHeader = serverRequest.headers().asHttpHeaders();
		return serverRequest.bodyToMono(APIRequestMessage.class)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("요청 body가 존재하지 않습니다."))) // body 없을 때 에러 처리
				.flatMap(request -> apiService.getInterfaceInfo(request.getInterfaceId()).flatMap(
						interfaceInfo -> rfcModuleService.rfcCallAndResponse(interfaceInfo, request).flatMap(result -> {
							return HttpHeaderUtil.makeDefaultResponseHeader(requestHeader, ESBStatusConstants.SUCCESS, "처리 성공").bodyValue(result);
						})))
				.onErrorResume(error -> {
					log.error("Error [rfc-sync-process] -> {}", error.getMessage(), error);
					return HttpHeaderUtil
							.makeDefaultResponseHeader(requestHeader, ESBStatusConstants.FAIL, error.getMessage())
							.bodyValue(APIResponseMessage.builder()
									.interfaceId(HttpHeaderUtil.getHeader(requestHeader, ESBAPIHeaderConstants.INTERFACE_ID))
									.transactionId(HttpHeaderUtil.getHeader(requestHeader, ESBAPIHeaderConstants.TRANSACTION_ID))
									.totalDataCount(HttpHeaderUtil.getIntHeader(requestHeader, ESBAPIHeaderConstants.TOTAL_COUNT))
									.errorDataCount(HttpHeaderUtil.getIntHeader(requestHeader, ESBAPIHeaderConstants.TOTAL_COUNT))
									.statusCode(ESBStatusConstants.FAIL).statusCode(error.getMessage()).build());
				});
	}

	public Mono<ServerResponse> rfcProcess(ServerRequest serverRequest) {
		String callBackPath = HttpHeaderUtil.getHeader(serverRequest.headers().asHttpHeaders(),
				ESBAPIHeaderConstants.CALL_BACK_PATH);
		return serverRequest.bodyToMono(APIRequestMessage.class)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("요청 body가 존재하지 않습니다."))) // body 없을 때 에러 처리
				.flatMap(request -> apiService.getInterfaceInfo(request.getInterfaceId())
						.flatMap(interfaceInfo -> rfcModuleService.rfcCallAndResponse(interfaceInfo, request)
								.doOnNext(result -> apiService.callGatewayForResult(result, callBackPath))))
				.onErrorMap(error -> {
					log.error("Error [rfc-process] -> {}", error.getMessage(), error); // 에러 처리
					return new RuntimeException(error.getMessage(), error);
				}).then(ServerResponse.ok().bodyValue("[rfc-process] 요청 수신 완료."));

	}
}
