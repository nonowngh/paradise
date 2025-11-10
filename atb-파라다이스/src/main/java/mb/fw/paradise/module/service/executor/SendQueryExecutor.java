package mb.fw.paradise.module.service.executor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.module.service.exception.SqlNotFoundException;
import mb.fw.paradise.module.service.executor.mapper.DynamicSqlMapper;

@Slf4j
@Service
@ConditionalOnAdaptorType(AdaptorType.DB)
public class SendQueryExecutor {

	private final DynamicSqlMapper dynamicQueryMapper;

	public SendQueryExecutor(DynamicSqlMapper dynamicQueryMapper) {
		this.dynamicQueryMapper = dynamicQueryMapper;
	}

	public int update(List<String> tableNameList, List<SqlQuery> queryList, Map<String, Object> params, String sqlId) {
		int updateCount = 0;
		for (String tableName : tableNameList) {
			try {
				String fullSqlId = sqlId + "." + tableName;
				int indiUpdateCount = dynamicQueryMapper.executeUpdate(queryList, fullSqlId, params);
				log.info("[{}] update table '{}' / count : {}", fullSqlId, tableName, indiUpdateCount);
				updateCount += indiUpdateCount;
			} catch (Exception e) {
				Throwable cause = e;
				while (cause.getCause() != null)
					cause = cause.getCause();
				if (cause instanceof SqlNotFoundException) {
					log.warn("Nothing sql-id [{}.{}] skip update.", sqlId, tableName);
				} else {
					log.error("Other error -> ", e);
				}
			}
		}
		return updateCount;
	}

	public LinkedHashMap<String, List<Map<String, Object>>> getTableData(List<String> sendTableNameList,
			List<String> recvTableNameList, List<SqlQuery> queryList, Map<String, Object> params, String sqlId)
			throws Exception {
		// 데이터 조회
		LinkedHashMap<String, List<Map<String, Object>>> tableItem = new LinkedHashMap<>();
		for (String tableName : sendTableNameList) {
			int index = sendTableNameList.indexOf(tableName);
			String putTableName = tableName;
			if (recvTableNameList != null) {
				putTableName = recvTableNameList.get(index);
			}
			String fullSqlId = sqlId + "." + tableName;
			List<Map<String, Object>> dataList = dynamicQueryMapper.executeSelectList(queryList, fullSqlId, params);
			log.info("[{}] select table '{}' / count : {}", fullSqlId, tableName, dataList.size());
			tableItem.put(putTableName, dataList);
		}
		return tableItem;
	}

}
