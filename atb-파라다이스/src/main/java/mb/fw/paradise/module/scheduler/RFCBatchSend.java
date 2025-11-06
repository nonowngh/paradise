package mb.fw.paradise.module.scheduler;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.PatternType;
import mb.fw.paradise.constants.TargetContextPathConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.module.BatchModule;
import mb.fw.paradise.module.service.RFCModuleService;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.util.DataItemUtil;
import mb.fw.paradise.util.TransactionGenerator;
import reactor.core.publisher.Mono;

@Slf4j
@Component("RFCBatchSend")
@ConditionalOnAdaptorType(AdaptorType.RFC)
public class RFCBatchSend implements BatchModule {

	private final APIService apiService;
	private final RFCModuleService rfcModuleService;

	public RFCBatchSend(APIService apiService, RFCModuleService rfcModuleService) {
		this.apiService = apiService;
		this.rfcModuleService = rfcModuleService;
	}

	@Override
	public void executeTask(String interfaceId) {
		String transactionId = TransactionIdGenerator.generate(interfaceId, TransactionGenerator.getNextSequence(),
				TransactionGenerator.getDateTimeNow());
		log.info("Batch scheduler start '{}' -> [{}]", interfaceId, transactionId);
		AtomicReference<InterfaceInfo> interfaceRef = new AtomicReference<>();
		apiService.getInterfaceInfo(interfaceId).flatMap(interfaceInfo -> {
			interfaceRef.set(interfaceInfo);
			String patternType = interfaceInfo.getPatternType();
			String targetPath = interfaceInfo.getRcvSystemCode()
					+ PatternType.fromPatternType(patternType).getTargetContextPath();
			String callBackPath = interfaceInfo.getSndSystemCode() + TargetContextPathConstants.RESULT_RFC_PROCESS;
			return rfcModuleService.rfcCallForSend(interfaceInfo, transactionId).flatMap(dataItem -> {
				int dataCount = DataItemUtil.tableDataCount(dataItem);
				if (dataCount == 0)
					return Mono.empty();
				log.info("Rfc export data count -> " + dataCount);
				return apiService.callGateway(
						APIRequestMessage.builder().interfaceId(interfaceId).transactionId(transactionId)
								.dataItem(dataItem).dataCount(dataCount).build(),
						targetPath, interfaceInfo.getSndSystemCode(), interfaceInfo.getRcvSystemCode(), callBackPath);
			}).switchIfEmpty(Mono.fromRunnable(() -> log.info("조회 데이터 없음")));
		}).doOnError(error -> log.error("오류 발생: {}", error.getMessage(), error))
//		.onErrorResume(error -> {
//			return rfcModuleService.rfcResult(
//					APIResponseMessage.builder().interfaceId(interfaceId).transactionId(transactionId)
//							.statusCode(ESBStatusConstants.FAIL).statusMessage(error.getMessage()).build(),
//					interfaceRef.get()).then(Mono.empty());
//		})
		.doFinally(signalType -> log.info("Batch scheduler end '{}' -> [{}]", interfaceId, transactionId))
				.subscribe(result -> log.debug("result -> {}", result));
	}
}
