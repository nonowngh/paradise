package mb.fw.paradise.module.service.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.api.model.PatternProperty;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.config.MyBatisConfig;
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
	private final MyBatisConfig config;

	public ReceiveQueryExecutor(DynamicSqlMapper dynamicQueryMapper, MyBatisConfig config) {
		this.dynamicQueryMapper = dynamicQueryMapper;
		this.config = config;
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
			int excuteCount = insertDataWithTransaction(data, queryList, expectedSqlId);
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
				List<Map<String, Object>> dataList = dynamicQueryMapper.executeSelectList(queryList,
						SQLConstants.SQL_ID_SELECT + "." + tableName, param);
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

	private int insertDataWithTransaction(List<Map<String, Object>> dataList, List<SqlQuery> queryList,
			String queryId) {
		if (dataList == null || dataList.isEmpty()) {
			return 0;
		}
		int totalInserted = 0;
		if (dataList.size() <= config.getThresholdCount()) {
			// 1000건 이하 → 반복 호출
			for (Map<String, Object> item : dataList) {
				totalInserted += dynamicQueryMapper.executeInsertList(queryList, queryId,
						Collections.singletonList(item));
			}
		} else {
			// 1000건 초과 → 배치 처리
			int fromIndex = 0;
			int dataSize = dataList.size();

			while (fromIndex < dataSize) {
				int toIndex = Math.min(fromIndex + config.getBatchSize(), dataSize);
				List<Map<String, Object>> subList = dataList.subList(fromIndex, toIndex);
				totalInserted += dynamicQueryMapper.executeInsertList(queryList, queryId, subList);
				fromIndex = toIndex;
			}
		}
		return totalInserted;
	}

}
