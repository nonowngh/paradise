package mb.fw.paradise.module.rfc.scheduler;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.paradise.common.base.BatchModule;
import mb.fw.paradise.common.config.ModuleConfig;
import mb.fw.paradise.common.constants.PatternType;
import mb.fw.paradise.common.constants.TargetContextPathConstants;
import mb.fw.paradise.common.dto.APIRequestMessage;
import mb.fw.paradise.common.service.APIService;
import mb.fw.paradise.common.util.DataItemUtil;
import mb.fw.paradise.common.util.TransactionGeneratorUtil;
import mb.fw.paradise.module.metaapi.model.MetaApiModel;
import mb.fw.paradise.module.rfc.service.RFCModuleService;
import reactor.core.publisher.Mono;

@Slf4j
@Component("RFCBatchSend")
public class RFCBatchSend implements BatchModule {

	private final APIService apiService;
	private final RFCModuleService rfcModuleService;
	private final ModuleConfig moduleConfig;

	public RFCBatchSend(APIService apiService, RFCModuleService rfcModuleService, ModuleConfig moduleConfig) {
		this.apiService = apiService;
		this.rfcModuleService = rfcModuleService;
		this.moduleConfig = moduleConfig;
	}

	@Override
	public void executeTask(String interfaceId) {
		String transactionId = TransactionIdGenerator.generate(interfaceId, TransactionGeneratorUtil.getNextSequence(),
				TransactionGeneratorUtil.getDateTimeNow());
		log.info("Batch scheduler start '{}' -> [{}]", interfaceId, transactionId);
		AtomicReference<MetaApiModel> interfaceRef = new AtomicReference<>();
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
				if (dataCount < moduleConfig.getLargeDataChunkSize())
					return apiService.callGateway(
							APIRequestMessage.builder().interfaceId(interfaceId).transactionId(transactionId)
									.dataItem(dataItem).dataCount(dataCount).build(),
							targetPath, interfaceInfo.getSndSystemCode(), interfaceInfo.getRcvSystemCode(),
							callBackPath);
				else
					return apiService.callGatewayLargeData(
							APIRequestMessage.builder().interfaceId(interfaceId).transactionId(transactionId)
									.dataItem(dataItem).dataCount(dataCount).build(),
							targetPath, interfaceInfo.getSndSystemCode(), interfaceInfo.getRcvSystemCode(),
							callBackPath, moduleConfig.getLargeDataChunkSize());
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
