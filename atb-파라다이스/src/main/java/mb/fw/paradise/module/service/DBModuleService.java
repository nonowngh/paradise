package mb.fw.paradise.module.service;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.api.model.PatternProperty;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.ESBCommonFieldConstants;
import mb.fw.paradise.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.constants.SQLConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.module.service.executor.SendQueryExecutor;
import mb.fw.paradise.util.HttpHeaderUtil;
import mb.fw.paradise.util.InterfaceInfoPropertyUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@ConditionalOnAdaptorType(AdaptorType.DB)
public class DBModuleService {

	@Autowired
	private SendQueryExecutor sendQueryExecutor;

	@Autowired
	private TransactionalService transactionalService;

	public Mono<APIResponseMessage> dbProcessAndResponse(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		return Mono.fromCallable(() -> transactionalService.transactionalProcess(interfaceInfo, request))
				.subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업 안전 처리
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
			String statusMessage = response.getStatusMessage();
			if (HttpHeaderUtil.isBase64(statusMessage))
				statusMessage = new String(Base64.getDecoder().decode(statusMessage), StandardCharsets.UTF_8);
			Map<String, Object> params = Stream.of(
					new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID, response.getInterfaceId()),
					new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID, response.getTransactionId()),
					new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_STATUS_CD, response.getStatusCode()),
					new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_STATUS_MSG,
							statusMessage == null ? "" : statusMessage))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return sendQueryExecutor.update(sendTableNameList, queryList, params, SQLConstants.SQL_ID_UPDATE_RESULT);
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
			return sendQueryExecutor.update(sendTableNameList, queryList, params, SQLConstants.SQL_ID_UPDATE);
		}).subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<DataItem> getSendData(InterfaceInfo interfaceInfo, String transactionId) {
		return Mono.fromCallable(() -> {
			List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
			List<String> sendTableNameList = InterfaceInfoPropertyUtil.getValueList(propertyList,
					InterfaceInfoPropertyConstants.SEND_TABLE_NAMES);
			List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
			Map<String, Object> params = Stream
					.of(new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_IF_ID,
							interfaceInfo.getInterfaceId()),
							new AbstractMap.SimpleEntry<>(ESBCommonFieldConstants.ESB_TX_ID, transactionId))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
							LinkedHashMap::new));
			List<String> recvTableNameList = Optional.ofNullable(propertyList)
					.filter(list -> InterfaceInfoPropertyUtil.existProperty(list,
							InterfaceInfoPropertyConstants.RECV_TABLE_NAMES))
					.map(list -> InterfaceInfoPropertyUtil.getValueList(list,
							InterfaceInfoPropertyConstants.RECV_TABLE_NAMES))
					.orElse(null);

			return DataItem.builder().param((LinkedHashMap<String, Object>) params).table(sendQueryExecutor
					.getTableData(sendTableNameList, recvTableNameList, queryList, params, SQLConstants.SQL_ID_SELECT))
					.build();
		}).subscribeOn(Schedulers.boundedElastic());
	}

}
