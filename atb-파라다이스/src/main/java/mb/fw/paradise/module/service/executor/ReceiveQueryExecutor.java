package mb.fw.paradise.module.service.executor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.api.model.PatternProperty;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.constants.SQLConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.module.service.executor.mapper.DynamicSqlMapper;
import mb.fw.paradise.util.InterfaceInfoPropertyUtil;

@Slf4j
@Service
@ConditionalOnAdaptorType(AdaptorType.DB)
public class ReceiveQueryExecutor {

	private final DynamicSqlMapper dynamicQueryMapper;

	public ReceiveQueryExecutor(DynamicSqlMapper dynamicQueryMapper) {
		this.dynamicQueryMapper = dynamicQueryMapper;
	}

	public void processDelete(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
		LinkedHashMap<String, List<Map<String, Object>>> tableData = request.getDataItem().getTable();
		LinkedHashMap<String, Object> param = request.getDataItem().getParam();
		tableData.forEach((tableName, data) -> {
			String expectedSqlId = SQLConstants.SQL_ID_DELETE + "." + tableName;
			int excuteCount = dynamicQueryMapper.executeDelete(queryList, expectedSqlId, param);
			log.info("delete table '{}' / count : {}", tableName, excuteCount);
		});
	}

	public void processInsert(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
		LinkedHashMap<String, List<Map<String, Object>>> tableData = request.getDataItem().getTable();
		tableData.forEach((tableName, data) -> {
			String expectedSqlId = SQLConstants.SQL_ID_INSERT + "." + tableName;
			int excuteCount = dynamicQueryMapper.executeInsertList(queryList, expectedSqlId, data);
			log.info("insert table '{}' / count : {}", tableName, excuteCount);
		});
	}

	public DataItem processSelect(InterfaceInfo interfaceInfo, APIRequestMessage request) throws Exception {
		List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
		LinkedHashMap<String, Object> param = request.getDataItem().getParam();
		List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
		LinkedHashMap<String, List<Map<String, Object>>> returnTableMap = new LinkedHashMap<>();
		if (InterfaceInfoPropertyUtil.existProperty(propertyList, InterfaceInfoPropertyConstants.RECV_TABLE_NAMES)) {
			List<String> tableNameList = InterfaceInfoPropertyUtil.getValueList(
					new ArrayList<>(interfaceInfo.getPropertyList()), InterfaceInfoPropertyConstants.RECV_TABLE_NAMES);
			tableNameList.forEach(tableName -> {
				List<Map<String, Object>> dataList = dynamicQueryMapper.executeSelectList(queryList, SQLConstants.SQL_ID_SELECT + "." + tableName, param);
				returnTableMap.put(tableName, dataList);
				log.info("select table '{}' / count : {}", tableName, dataList.size());
			});
		} else {
			String expectedSqlId = SQLConstants.SQL_ID_SELECT;
			List<Map<String, Object>> dataList = dynamicQueryMapper.executeSelectList(queryList, expectedSqlId, param);
			returnTableMap.put("data", dataList);
			log.info("select single table / count : {}", dataList.size());
		}
		return DataItem.builder().table(returnTableMap).param(param).build();
	}

}
