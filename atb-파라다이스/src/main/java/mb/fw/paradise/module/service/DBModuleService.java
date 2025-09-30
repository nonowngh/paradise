package mb.fw.paradise.module.service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@ConditionalOnBean(DataSource.class)
public class DBModuleService {

	@Autowired
	private ReceiveQueryExecutor receiveQueryExecutor;

	@Autowired
	private SendQueryExecutor sendQueryExecutor;

	@Transactional
	public Mono<APIResponseMessage> dbProcessAndResponse(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		return Mono.fromCallable(() -> {
			receiveQueryExecutor.processInsertQueries(interfaceInfo, request);
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
			List<String> tableNameList = propertyList.stream()
					.filter(property -> InterfaceInfoPropertyConstants.DB_SEND_TABLE_NAMES.equals(property.getPropertyName()))
					.flatMap(p -> Arrays.stream(p.getPropertyValue().split(","))).map(String::trim)
					.filter(s -> !s.isEmpty()).collect(Collectors.toList());
			List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
			Map<String, Object> params = Stream
					.of(new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID, response.getInterfaceId()),
							new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID,
									response.getTransactionId()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return sendQueryExecutor.resultUpdate(tableNameList, queryList, params);
		}).subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<Integer> markSendData(InterfaceInfo interfaceInfo, String transactionId) {
		return Mono.fromCallable(() -> {
			List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
			List<String> tableNameList = propertyList.stream()
					.filter(property -> InterfaceInfoPropertyConstants.DB_SEND_TABLE_NAMES.equals(property.getPropertyName()))
					.flatMap(p -> Arrays.stream(p.getPropertyValue().split(","))).map(String::trim)
					.filter(s -> !s.isEmpty()).collect(Collectors.toList());
			List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
			Map<String, Object> params = Stream
					.of(new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID,
							interfaceInfo.getInterfaceId()),
							new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID, transactionId))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return sendQueryExecutor.update(tableNameList, queryList, params);
		}).subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<DataItem> getSendData(InterfaceInfo interfaceInfo, String transactionId) {
		return Mono.fromCallable(() -> {
			List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
			List<String> tableNameList = propertyList.stream()
					.filter(property -> InterfaceInfoPropertyConstants.DB_SEND_TABLE_NAMES.equals(property.getPropertyName()))
					.flatMap(p -> Arrays.stream(p.getPropertyValue().split(","))).map(String::trim)
					.filter(s -> !s.isEmpty()).collect(Collectors.toList());
			List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
			Map<String, Object> params = Stream
					.of(new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID,
							interfaceInfo.getInterfaceId()),
							new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID, transactionId))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return DataItem.builder().table(sendQueryExecutor.getTableData(tableNameList, queryList, params)).build();
		}).subscribeOn(Schedulers.boundedElastic());
	}

}
