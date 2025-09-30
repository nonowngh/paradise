package mb.fw.paradise.module.scheduler;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.paradise.constants.PatternType;
import mb.fw.paradise.constants.TargetContextPathConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.module.BatchModule;
import mb.fw.paradise.module.service.DBModuleService;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.util.TransactionGenerator;
import reactor.core.publisher.Mono;

@Slf4j
@Component("DBPollingAndSend")
public class DBPollingAndSend implements BatchModule {

	private final APIService apiService;
	private final DBModuleService dbModuleService;

	public DBPollingAndSend(APIService apiService, DBModuleService dbModuleService) {
		this.apiService = apiService;
		this.dbModuleService = dbModuleService;
	}

	@Override
	public void executeTask(String interfaceId) {
		String transactionId = TransactionIdGenerator.generate(interfaceId, TransactionGenerator.getNextSequence(),
				TransactionGenerator.getDateTimeNow());
		log.info("Batch interface start '{}' -> [{}]", interfaceId, transactionId);

		apiService.getInterfaceInfo(interfaceId).flatMap(
				interfaceInfo -> dbModuleService.markSendData(interfaceInfo, transactionId).flatMap(updateCount -> {
					if (updateCount <= 0) {
						log.info("조회 데이터 없음");
						return Mono.empty();
					}
					log.info("업데이트된 행 수: {}", updateCount);
					String patternCode = interfaceInfo.getPatternCode();
					String callBackPath = interfaceInfo.getSndSystemCode()
							+ TargetContextPathConstants.RESULT_DB_PROCESS;
					return dbModuleService.getSendData(interfaceInfo, transactionId)
							.flatMap(dataItem -> apiService.callGateway(
									APIRequestMessage.builder().interfaceId(interfaceId).transactionId(transactionId)
											.dataItem(dataItem).totalDataCount(updateCount).build(),
									PatternType.fromPatternType(patternCode), interfaceInfo.getSndSystemCode(),
									interfaceInfo.getRcvSystemCode(), callBackPath));
				})).doOnError(e -> log.error("오류 발생: {}", e.getMessage(), e)).subscribe();
		log.info("Batch interface end '{}' -> [{}]", interfaceId, transactionId);
	}
}
