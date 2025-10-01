package mb.fw.paradise.module.service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.api.model.PatternProperty;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.constants.ESBCommonFieldConstants;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.module.service.sqlexecutor.ReceiveQueryExecutor;
import mb.fw.paradise.module.service.sqlexecutor.SendQueryExecutor;
import mb.fw.paradise.util.InterfaceInfoPropertyUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class DBModuleService {

	@Autowired
	private ReceiveQueryExecutor receiveQueryExecutor;

	@Autowired
	private SendQueryExecutor sendQueryExecutor;

	@Transactional
	public Mono<APIResponseMessage> dbProcessAndResponse(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		return Mono.fromCallable(() -> {
			List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
			String workType = InterfaceInfoPropertyUtil.getValue(propertyList,
					InterfaceInfoPropertyConstants.DB_WORK_TYPE);
			for (char ch : workType.toCharArray()) {
				switch (ch) {
				case 'D':
					receiveQueryExecutor.processDelete(interfaceInfo, request);
					break;
				case 'I':
					receiveQueryExecutor.processInsert(interfaceInfo, request);
					break;
//				case 'U':
//					receiveQueryExecutor.processUpdateQueries(interfaceInfo, request);
//					break;
//				case 'P':
//					receiveQueryExecutor.processProcedureQueries(interfaceInfo, request);
//					break;
				default:
					throw new IllegalArgumentException("Invalid 'workType' : " + ch);
				}
			}
			return APIResponseMessage.builder().statusCode(ESBStatusConstants.SUCCESS)
					.interfaceId(request.getInterfaceId()).transactionId(request.getTransactionId())
					.totalDataCount(request.getTotalDataCount()).build();
		}).subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업 안전 처리
				.onErrorResume(e -> {
					return Mono.error(new RuntimeException("DB 처리 실패: " + e.getMessage(), e));
				});
	}

	public Mono<Integer> dbResult(APIResponseMessage response, InterfaceInfo interfaceInfo) {
		return Mono.fromCallable(() -> {
			List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
			List<String> sendTableNameList = InterfaceInfoPropertyUtil.getValueList(propertyList,
					InterfaceInfoPropertyConstants.SEND_TABLE_NAMES);
			List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
			Map<String, Object> params = Stream
					.of(new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID, response.getInterfaceId()),
							new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID,
									response.getTransactionId()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return sendQueryExecutor.resultUpdate(sendTableNameList, queryList, params);
		}).subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<Integer> markSendData(InterfaceInfo interfaceInfo, String transactionId) {
		return Mono.fromCallable(() -> {
			List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
			List<String> sendTableNameList = InterfaceInfoPropertyUtil.getValueList(propertyList,
					InterfaceInfoPropertyConstants.SEND_TABLE_NAMES);
			List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
			Map<String, Object> params = Stream
					.of(new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID,
							interfaceInfo.getInterfaceId()),
							new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID, transactionId))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return sendQueryExecutor.update(sendTableNameList, queryList, params);
		}).subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<DataItem> getSendData(InterfaceInfo interfaceInfo, String transactionId) {
		return Mono.fromCallable(() -> {
			List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
			List<String> sendTableNameList = InterfaceInfoPropertyUtil.getValueList(propertyList,
					InterfaceInfoPropertyConstants.SEND_TABLE_NAMES);
			List<String> recvTableNameList = InterfaceInfoPropertyUtil.getValueList(propertyList,
					InterfaceInfoPropertyConstants.RECV_TABLE_NAMES);
			List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
			Map<String, Object> params = Stream
					.of(new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID,
							interfaceInfo.getInterfaceId()),
							new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID, transactionId))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return DataItem.builder()
					.table(sendQueryExecutor.getTableData(sendTableNameList, recvTableNameList, queryList, params))
					.build();
		}).subscribeOn(Schedulers.boundedElastic());
	}

}
