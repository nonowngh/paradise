package mb.fw.paradise.module.send;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.constants.ESBCommonFieldConstants;
import mb.fw.paradise.constants.TargetModule;
import mb.fw.paradise.dto.APIReqeustMessage;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.module.BatchModule;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.service.SndModuleService;
import mb.fw.paradise.util.TransactionGenerator;

@Slf4j
@Component("DBPollingAndSend")
public class DBPollingAndSend implements BatchModule {

	private final APIService apiService;
	private final SndModuleService sendModuleService;

	public DBPollingAndSend(APIService apiService, SndModuleService sendModuleService) {
		this.apiService = apiService;
		this.sendModuleService = sendModuleService;
	}

	@Override
	public void executeTask(String interfaceId) {
		String transactionId = TransactionIdGenerator.generate(interfaceId, TransactionGenerator.getNextSequence(),
				TransactionGenerator.getDateTimeNow());
		log.info("interface '{}' excute -> [{}]", interfaceId, transactionId);

		InterfaceInfo interfaceInfo = apiService.getInterfaceInfo(interfaceId);
		String targetPatternType = interfaceInfo.getTargetPatternType();
		List<String> tableNameList = Arrays.stream(interfaceInfo.getSndTableNames().split(",")).map(String::trim)
				.collect(Collectors.toList());
		List<SqlQuery> queryList = interfaceInfo.getSqlQueryList();

		Map<String, Object> params = Stream
				.of(new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID, interfaceId),
						new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID, transactionId))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		if (sendModuleService.update(tableNameList, queryList, params) > 0) {
			log.info("송신할 데이터 없음...");
			return;
		}

		apiService
				.callGateway(APIReqeustMessage.builder().interfaceId(interfaceId)
						.dataItem(DataItem.builder()
								.table(sendModuleService.getTableData(tableNameList, queryList, params)).build())
						.build(), TargetModule.fromPatternType(targetPatternType))
				.subscribe(result -> log.info("송신 완료: {}", result),
						error -> log.error("송신 오류: {}", error.getMessage(), error));
	}

}
