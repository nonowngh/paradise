package mb.fw.paradise.module.server.handler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoRecord;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.api.model.PatternProperty;
import mb.fw.paradise.constants.ESBCommonFieldConstants;
import mb.fw.paradise.constants.ESBStatusConstants;
import mb.fw.paradise.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.constants.PatternType;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.module.service.executor.JcoExecutor;
import mb.fw.paradise.service.APIService;
import mb.fw.paradise.util.DataItemUtil;
import mb.fw.paradise.util.InterfaceInfoPropertyUtil;
import mb.fw.paradise.util.TransactionGeneratorUtil;

@Slf4j
public class RfcFunctionHandler implements JCoServerFunctionHandler {
	private final Properties sapProperties;
	private final List<String> interfaceIdList;
	private final APIService apiService;

	public RfcFunctionHandler(Properties sapProperties, List<String> interfaceIdList, APIService apiService) {
		this.sapProperties = sapProperties;
		this.interfaceIdList = interfaceIdList;
		this.apiService = apiService;
	}

	@Override
	public void handleRequest(JCoServerContext ctx, JCoFunction function) {
		
		InterfaceInfo interfaceInfo = null;
		String transactionId = "";
		try {
			String functionName = function.getName();
			String clientId = ctx.getConnectionAttributes().getClient();
			log.info("RFC({}) from SAP client={}", functionName, clientId);

			// 1) 인터페이스 정보 조회
			List<InterfaceInfo> interfaceInfoList = loadInterfaceInfoList(functionName);

			// 2) 대상 Interface 선택
			interfaceInfo = selectInterface(function, interfaceInfoList);

			// 3) 트랜잭션 ID 생성
			transactionId = TransactionIdGenerator.generate(interfaceInfo.getInterfaceId(),
					TransactionGeneratorUtil.getNextSequence(), TransactionGeneratorUtil.getDateTimeNow());

			// 4) SAP Client 검증
			validateClientId(clientId);
			
			// 5) Import 데이터 추출
			DataItem dataItem = extractImportData(function, interfaceInfo);
			int dataCount = DataItemUtil.tableDataCount(dataItem);
			log.debug("receive from rfc[{}] : {}", functionName, dataItem);
			log.info("RFC send data count : {}", dataCount);

			// 6) Gateway 호출
			APIResponseMessage response = callGateway(interfaceInfo, transactionId, dataItem, dataCount);
			log.info("Gateway Response: {}", response);

			// 7) SAP에 Export
			exportToSapSuccess(function, response, transactionId, interfaceInfo);

		} catch (Exception e) {
			log.error("RFC Handler Exception", e);
			exportToSapError(function, e, transactionId, interfaceInfo);
		} finally {
			log.info("{}['{}'] End.", function.getName(), transactionId);
		}
	}

	private List<InterfaceInfo> loadInterfaceInfoList(String functionName) {
		List<InterfaceInfo> list = apiService.getInterfaceInfoByFunctionName(functionName, interfaceIdList).block();
		if (list == null || list.isEmpty()) {
			throw new NoSuchElementException("No interface config for RFC '" + functionName + "'");
		}
		return list;
	}

	private InterfaceInfo selectInterface(JCoFunction function, List<InterfaceInfo> interfaceList) throws Exception {
		String targetSysCode = extractTargetSysCode(function);
		log.info("targetSysCode=[{}], RFC={}", targetSysCode, function.getName());
		for (InterfaceInfo info : interfaceList) {
			List<PatternProperty> propertyList = new ArrayList<>(info.getPropertyList());
			String legCode = InterfaceInfoPropertyUtil.getValue(propertyList,
					InterfaceInfoPropertyConstants.RFC_TARGET_LEG_CODE);
			if (targetSysCode.equals(legCode) || targetSysCode.isEmpty()) {
				return info;
			}
		}
		throw new Exception("Matching InterfaceInfo not found. TargetSysCode=" + targetSysCode);
	}

	private String extractTargetSysCode(JCoFunction function) {
		try {
			JCoParameterList importList = function.getImportParameterList();
			return importList.getString(ESBCommonFieldConstants.RFC_TARGET_SYS_CD);
		} catch (Exception e) {
			log.warn("TARGET_SYS_CODE not found in import parameter");
			return "";
		}
	}

	private void validateClientId(String clientId) {
		String expected = sapProperties.getProperty(DestinationDataProvider.JCO_CLIENT);
		if (!clientId.equalsIgnoreCase(expected)) {
			throw new IllegalArgumentException("Invalid SAP ClientId = " + clientId);
		}
	}

	private DataItem extractImportData(JCoFunction function, InterfaceInfo interfaceInfo) {
		return JcoExecutor.exportData(function.getImportParameterList(), function.getTableParameterList(),
				new ArrayList<>(interfaceInfo.getPropertyList()));
	}

	private APIResponseMessage callGateway(InterfaceInfo interfaceInfo, String txId, DataItem dataItem, int dataCount) {
		String targetPath = interfaceInfo.getRcvSystemCode()
				+ PatternType.fromPatternType(interfaceInfo.getPatternType()).getTargetContextPath();
		return apiService.callGatewaySync(
				APIRequestMessage.builder().interfaceId(interfaceInfo.getInterfaceId()).transactionId(txId)
						.dataItem(dataItem).dataCount(dataCount).build(),
				targetPath, interfaceInfo.getSndSystemCode(), interfaceInfo.getRcvSystemCode()).block();
	}

	private void exportToSapSuccess(JCoFunction function, APIResponseMessage response, String txId, InterfaceInfo info)
			throws Exception {
		String status = ESBStatusConstants.FAIL.equals(response.getStatusCode()) ? ESBStatusConstants.FAIL_RFC
				: response.getStatusCode();
		fillBasicExport(function, txId, status, response.getStatusMessage());
		fillStructureExport(function, info, txId, response.getStatusMessage(), status);
		fillTableExport(function, info, response);
	}

	private void exportToSapError(JCoFunction function, Exception e, String txId, InterfaceInfo info) {
		try {
			fillBasicExport(function, txId, ESBStatusConstants.FAIL_RFC, e.getMessage());
			fillStructureExport(function, info, txId, e.getMessage(), ESBStatusConstants.FAIL_RFC);
		} catch (Exception ex) {
			log.error("exportToSapError Exception", ex);
		}
	}

	private void fillBasicExport(JCoFunction f, String txId, String status, String msg) {
		JCoParameterList export = f.getExportParameterList();
		for (int i = 0; i < export.getMetaData().getFieldCount(); i++) {
			String name = export.getMetaData().getName(i);
			switch (name) {
			case ESBCommonFieldConstants.ESB_TX_ID:
				export.setValue(name, txId);
				break;
			case ESBCommonFieldConstants.RFC_PARAMETER_MTYPE:
			case ESBCommonFieldConstants.RFC_PARAMETER_RETURN:
				export.setValue(name, status);
				break;
			case ESBCommonFieldConstants.RFC_PARAMETER_MESSAGE:
				export.setValue(name, msg);
				break;
			}
		}
		log.debug("return to rfc : [{}], [{}], [{}]", txId, status, msg);
	}

	private void fillStructureExport(JCoFunction function, InterfaceInfo info, String txId, String message,
			String status) {
		if (info == null)
			return;
		List<PatternProperty> propertyList = new ArrayList<>(info.getPropertyList());
		if (!InterfaceInfoPropertyUtil.existProperty(propertyList,
				InterfaceInfoPropertyConstants.RFC_RETURN_STRUCTURE_NAME))
			return;
		String structName = InterfaceInfoPropertyUtil.getValue(propertyList,
				InterfaceInfoPropertyConstants.RFC_RETURN_STRUCTURE_NAME);
		JCoStructure structure = function.getExportParameterList().getStructure(structName);
		if (structure != null) {
			setStructureRecord(structure, txId, message, status);
		}
	}

	private void setStructureRecord(JCoRecord rec, String txId, String message, String status) {
		set(rec, ESBCommonFieldConstants.ESB_TX_ID, txId);
		set(rec, ESBCommonFieldConstants.ESB_TIME, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		set(rec, ESBCommonFieldConstants.ESB_DATE, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
		set(rec, ESBCommonFieldConstants.ESB_STATUS_CD, status);
		set(rec, ESBCommonFieldConstants.ESB_STATUS_MSG, message);
	}

	private void fillTableExport(JCoFunction function, InterfaceInfo info, APIResponseMessage response)
			throws Exception {
		if (info == null || response.getResultItem() == null)
			return;
		List<PatternProperty> propertyList = new ArrayList<>(info.getPropertyList());
		if (!InterfaceInfoPropertyUtil.existProperty(propertyList,
				InterfaceInfoPropertyConstants.RFC_RETURN_TABLE_NAME))
			return;
		String tableName = InterfaceInfoPropertyUtil.getValue(propertyList,
				InterfaceInfoPropertyConstants.RFC_RETURN_TABLE_NAME);
		JCoTable table = function.getTableParameterList().getTable(tableName);
		List<Map<String, Object>> tableData = response.getResultItem().getTable().get(tableName);
		if (table == null || tableData == null)
			return;
		List<String> keys = getKeyListFromTable(table);
		for (Map<String, Object> row : tableData) {
			table.appendRow();
			for (String key : keys) {
				table.setValue(key, row.getOrDefault(key, row.get(key.toLowerCase())));
			}
		}
	}

	private void set(JCoRecord rec, String key, String val) {
		rec.setValue(key, val);
		log.debug("structure [{}/{}]", key, val);
	}

	private static List<String> getKeyListFromTable(JCoTable table) {
		List<String> result = new ArrayList<>();
		JCoRecordMetaData meta = table.getRecordMetaData();
		for (int i = 0; i < meta.getFieldCount(); i++) {
			result.add(meta.getName(i));
		}
		return result;
	}
}
