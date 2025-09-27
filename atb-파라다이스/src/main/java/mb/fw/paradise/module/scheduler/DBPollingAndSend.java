package mb.fw.paradise.module.scheduler;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.constants.ESBCommonFieldConstants;
import mb.fw.paradise.constants.PatternType;
import mb.fw.paradise.constants.TargetContextPathConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.module.BatchModule;
import mb.fw.paradise.module.service.SendDBModuleService;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.util.TransactionGenerator;
import reactor.core.publisher.Mono;

@Slf4j
@Component("DBPollingAndSend")
public class DBPollingAndSend implements BatchModule {

	private final APIService apiService;
	private final SendDBModuleService sendDBModuleService;

	public DBPollingAndSend(APIService apiService, SendDBModuleService sendDBModuleService) {
		this.apiService = apiService;
		this.sendDBModuleService = sendDBModuleService;
	}

	@Override
	public void executeTask(String interfaceId) {
		String transactionId = TransactionIdGenerator.generate(interfaceId, TransactionGenerator.getNextSequence(),
				TransactionGenerator.getDateTimeNow());
		log.info("Batch interface start '{}' -> [{}]", interfaceId, transactionId);

		apiService.getInterfaceInfo(interfaceId).flatMap(interfaceInfo -> {
			String patternCode = interfaceInfo.getPatternCode();
			List<String> tableNameList = Arrays.stream(interfaceInfo.getSndTableNames().split(",")).map(String::trim)
					.collect(Collectors.toList());
			List<SqlQuery> queryList = interfaceInfo.getSqlQueryList();
			// 필수 파라미터 설정
			Map<String, Object> params = Stream
					.of(new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID, interfaceId),
							new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID, transactionId))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			// 데이터 업데이트 'N' -> 'P'
			if (sendDBModuleService.update(tableNameList, queryList, params) > 0) {
				log.info("송신할 데이터 없음.");
				return Mono.empty();
			}
			// 데이터 조회 후 Gateway 호출
			APIRequestMessage request = APIRequestMessage.builder().interfaceId(interfaceId)
					.transactionId(transactionId)
					.dataItem(DataItem.builder()
							.table(sendDBModuleService.getTableData(tableNameList, queryList, params)).build())
					.callBackPath(interfaceInfo.getSndSystemCode() + TargetContextPathConstants.RESULT_DB_PROCESS)
					.build();

			return apiService
					.callGateway(request, PatternType.fromPatternType(patternCode), interfaceInfo.getRcvSystemCode())
					.doOnSuccess(result -> log.info("송신 완료: {}", result));
		}).subscribe();

		log.info("Batch interface end '{}' -> [{}]", interfaceId, transactionId);
	}

}
