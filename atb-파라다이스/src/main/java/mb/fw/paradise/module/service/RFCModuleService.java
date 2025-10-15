package mb.fw.paradise.module.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.module.service.sqlexecutor.JcoExecutor;
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

	public Mono<APIResponseMessage> rfcProcess(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		return Mono.fromCallable(() -> {
			String functionName = InterfaceInfoPropertyUtil.getValue(new ArrayList<>(interfaceInfo.getPropertyList()),
					InterfaceInfoPropertyConstants.RFC_FUNCTION_NAME);
			List<String> exportTableList = InterfaceInfoPropertyUtil.getValueList(
					new ArrayList<>(interfaceInfo.getPropertyList()),
					InterfaceInfoPropertyConstants.RFC_EXPORT_TABLE_NAMES);
			JCoFunction function = jcoDestination.getRepository().getFunction(functionName);
			if (function == null)
				throw new RuntimeException("RFC Function not found! -> " + functionName);
			JCoParameterList paramList = function.getImportParameterList();
			JCoParameterList tableParamList = function.getTableParameterList();
			DataItem dataItem = request.getDataItem();
			JcoExecutor.importParms(dataItem.getParameter(), paramList);
			JcoExecutor.importTables(dataItem.getTable(), tableParamList);
			log.info("call function : {}", function.getName());
			function.execute(jcoDestination);
			DataItem ResultItem = JcoExecutor.exportData(function.getExportParameterList(), tableParamList,
					exportTableList);
//			int resultCount = JcoExecutor.getResultCount(); 이거 만들어서 reponse DataCount에 넣기
			return APIResponseMessage.builder().statusCode(ESBStatusConstants.SUCCESS)
					.interfaceId(request.getInterfaceId()).transactionId(request.getTransactionId())
					.totalDataCount(request.getTotalDataCount()).resultItem(ResultItem).build();
		}).subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업 안전 처리
				.onErrorResume(e -> {
					return Mono.error(new RuntimeException("RFC 처리 실패: " + e.getMessage(), e));
				});

	}
}
