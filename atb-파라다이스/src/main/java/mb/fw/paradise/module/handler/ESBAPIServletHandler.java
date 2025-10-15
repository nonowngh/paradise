package mb.fw.paradise.module.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.constants.PatternType;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.util.TransactionGenerator;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@ConditionalOnAdaptorType(AdaptorType.API)
public class ESBAPIServletHandler {

	private final APIService apiService;

	public ESBAPIServletHandler(APIService apiService) {
		this.apiService = apiService;
	}

	public Mono<ServerResponse> callGateway(ServerRequest serverRequest) {
		return serverRequest.bodyToMono(APIRequestMessage.class)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("요청 body가 존재하지 않습니다."))) // body 없을 때 에러 처리
				.flatMap(request -> apiService.getInterfaceInfo(request.getInterfaceId()).flatMap(interfaceInfo -> {
					String patternType = interfaceInfo.getPatternType();
					String targetPath = interfaceInfo.getRcvSystemCode()
							+ PatternType.fromPatternType(patternType).getTargetContextPath();
					String interfaceId = interfaceInfo.getInterfaceId();
					String transactionId = TransactionIdGenerator.generate(interfaceId,
							TransactionGenerator.getNextSequence(), TransactionGenerator.getDateTimeNow());
					request.setTransactionId(transactionId);
					request.setTotalDataCount(1);
					return apiService.callGatewaySync(request, targetPath, interfaceInfo.getSndSystemCode(),
							interfaceInfo.getRcvSystemCode());
				}).flatMap(response -> ServerResponse.ok().bodyValue(response)).onErrorResume(error -> {
					log.error("Error [API call-gateway process] -> {}", error.getMessage(), error);
					return ServerResponse.status(500)
							.bodyValue(APIResponseMessage.builder().interfaceId(request.getInterfaceId())
									.resultItem(request.getDataItem()).statusCode(ESBStatusConstants.FAIL)
									.statusMessage(error.getMessage()).build());
				}));
	}
}
