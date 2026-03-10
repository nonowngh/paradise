package mb.fw.paradise.module.rfc.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.common.constants.ESBStatusConstants;
import mb.fw.paradise.common.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.common.dto.APIRequestMessage;
import mb.fw.paradise.common.dto.APIResponseMessage;
import mb.fw.paradise.common.dto.DataItem;
import mb.fw.paradise.common.util.DataItemUtil;
import mb.fw.paradise.common.util.InterfaceInfoPropertyUtil;
import mb.fw.paradise.module.db.service.executor.JcoExecutor;
import mb.fw.paradise.module.metaapi.model.MetaApiModel;
import mb.fw.paradise.module.metaapi.model.PatternProperty;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class RFCModuleService {

	private final JCoDestination jcoDestination;

	public RFCModuleService(@Qualifier("jcoDestinationClient") JCoDestination jcoDestination) {
		this.jcoDestination = jcoDestination;
	}

	public Mono<APIResponseMessage> rfcCallForReceive(MetaApiModel interfaceInfo, APIRequestMessage request) {
		return Mono.fromCallable(() -> {
			List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
			String functionName = InterfaceInfoPropertyUtil.getValue(propertyList,
					InterfaceInfoPropertyConstants.RFC_FUNCTION_NAME);
			JCoFunction function = jcoDestination.getRepository().getFunction(functionName);
			if (function == null)
				throw new RuntimeException("RFC Function not found! -> " + functionName);
			JCoParameterList paramList = function.getImportParameterList();
			JCoParameterList tableParamList = function.getTableParameterList();
			DataItem dataItem = request.getDataItem();
			JcoExecutor.importParms(dataItem.getParam(), paramList, propertyList);
			JcoExecutor.importTables(dataItem.getTable(), tableParamList, propertyList);
			log.info("call function : {}", function.getName());
			function.execute(jcoDestination);
			DataItem resultItem = JcoExecutor.exportData(function.getExportParameterList(), tableParamList,
					propertyList);
			log.info("rfc call result item : {}", resultItem);
			return APIResponseMessage.builder().statusCode(ESBStatusConstants.SUCCESS)
					.interfaceId(request.getInterfaceId()).transactionId(request.getTransactionId())
					.dataCount(DataItemUtil.tableDataCount(resultItem)).resultItem(resultItem).build();
		}).subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업 안전 처리
				.onErrorResume(e -> {
					return Mono.error(new RuntimeException("RFC 처리 실패: " + e.getMessage(), e));
				});
	}

	public Mono<DataItem> rfcCallForSend(MetaApiModel interfaceInfo, String transactionId) {
		return Mono.fromCallable(() -> {
			List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
			String functionName = InterfaceInfoPropertyUtil.getValue(propertyList,
					InterfaceInfoPropertyConstants.RFC_FUNCTION_NAME);
			JCoFunction function = jcoDestination.getRepository().getFunction(functionName);
			if (function == null)
				throw new RuntimeException("RFC Function not found! -> " + functionName);
			Map<String, Object> importParamMap = new LinkedHashMap<>();
			if (InterfaceInfoPropertyUtil.existProperty(propertyList,
					InterfaceInfoPropertyConstants.RFC_IMPORT_FIXED_PARAMETER)) {
				importParamMap = InterfaceInfoPropertyUtil.getValueMap(propertyList,
						InterfaceInfoPropertyConstants.RFC_IMPORT_FIXED_PARAMETER);
			}
			JCoParameterList paramList = function.getImportParameterList();
			JCoParameterList tableParamList = function.getTableParameterList();
			log.info("rfc call request data : {}", importParamMap);
			JcoExecutor.importParms((LinkedHashMap<String, Object>) importParamMap, paramList, propertyList);
			log.info("call function : {}", function.getName());
			function.execute(jcoDestination);
//			log.info("rfc call return data : {}", dataItem);
			return JcoExecutor.exportData(function.getExportParameterList(), tableParamList, propertyList);
		}).subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<String> rfcResult(APIResponseMessage response, MetaApiModel interfaceInfo) {
		return Mono.just("RFC result process complete...");
	}

}
