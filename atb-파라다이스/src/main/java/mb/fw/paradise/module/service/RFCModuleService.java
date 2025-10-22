package mb.fw.paradise.module.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.api.model.PatternProperty;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.module.service.executor.JcoExecutor;
import mb.fw.paradise.util.DataItemUtil;
import mb.fw.paradise.util.InterfaceInfoPropertyUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@ConditionalOnAdaptorType(AdaptorType.RFC)
public class RFCModuleService {

	private final JCoDestination jcoDestination;

	public RFCModuleService(JCoDestination jcoDestination) {
		this.jcoDestination = jcoDestination;
	}

	public Mono<APIResponseMessage> rfcCallAndResponse(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		return Mono.fromCallable(() -> {
			List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
			String functionName = InterfaceInfoPropertyUtil.getValue(propertyList,
					InterfaceInfoPropertyConstants.RFC_FUNCTION_NAME);
			List<String> exportTableList = InterfaceInfoPropertyUtil.getValueList(propertyList,
					InterfaceInfoPropertyConstants.RFC_EXPORT_TABLE_NAMES);
			JCoFunction function = jcoDestination.getRepository().getFunction(functionName);
			if (function == null)
				throw new RuntimeException("RFC Function not found! -> " + functionName);
			JCoParameterList paramList = function.getImportParameterList();
			JCoParameterList tableParamList = function.getTableParameterList();
			DataItem dataItem = request.getDataItem();
			JcoExecutor.importParms(dataItem.getParameter(), paramList, propertyList);
			JcoExecutor.importTables(dataItem.getTable(), tableParamList, propertyList);
			log.info("call function : {}", function.getName());
			function.execute(jcoDestination);
			DataItem resultItem = JcoExecutor.exportData(function.getExportParameterList(), tableParamList,
					exportTableList);
			return APIResponseMessage.builder().statusCode(ESBStatusConstants.SUCCESS)
					.interfaceId(request.getInterfaceId()).transactionId(request.getTransactionId())
					.dataCount(DataItemUtil.tableDataCount(resultItem)).resultItem(resultItem).build();
		}).subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업 안전 처리
				.onErrorResume(e -> {
					return Mono.error(new RuntimeException("RFC 처리 실패: " + e.getMessage(), e));
				});

	}

}
