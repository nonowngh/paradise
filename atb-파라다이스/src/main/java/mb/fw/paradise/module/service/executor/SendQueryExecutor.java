package mb.fw.paradise.module.service.executor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.SQLConstants;
import mb.fw.paradise.module.service.executor.exception.SqlNotFoundException;
import mb.fw.paradise.module.service.executor.mapper.DynamicSqlMapper;

@Slf4j
@Service
@ConditionalOnAdaptorType(AdaptorType.DB)
public class SendQueryExecutor {

	private final DynamicSqlMapper dynamicQueryMapper;

	public SendQueryExecutor(DynamicSqlMapper dynamicQueryMapper) {
		this.dynamicQueryMapper = dynamicQueryMapper;
	}

	public int update(List<String> tableNameList, List<SqlQuery> queryList, Map<String, Object> params) {
		int updateCount = 0;
		for (String tableName : tableNameList) {
			try {
				int indiUpdateCount = dynamicQueryMapper.executeUpdate(queryList,
						SQLConstants.SQL_ID_UPDATE + "." + tableName, params);
				log.info("update table '{}' / count : {}", tableName, indiUpdateCount);
				updateCount += indiUpdateCount;
			} catch (Exception e) {
				Throwable cause = e;
				while(cause.getCause() != null) cause = cause.getCause();
				if (cause instanceof SqlNotFoundException) {
					log.warn("Nothing sql-id [{}.{}] skip update.", SQLConstants.SQL_ID_UPDATE, tableName);
				} else {
					log.error("Other error -> ", e);
				}
			}
		}
		return updateCount;
	}

	public LinkedHashMap<String, List<Map<String, Object>>> getTableData(List<String> sendTableNameList,
			List<String> recvTableNameList, List<SqlQuery> queryList, Map<String, Object> params) throws Exception {
		// 데이터 조회
		LinkedHashMap<String, List<Map<String, Object>>> tableItem = new LinkedHashMap<>();
		for (String tableName : sendTableNameList) {
			int index = sendTableNameList.indexOf(tableName);
			String putTableName = tableName;
			if (recvTableNameList != null) {
				putTableName = recvTableNameList.get(index);
			}
			List<Map<String, Object>> dataList = dynamicQueryMapper.executeSelectList(queryList,
					SQLConstants.SQL_ID_SELECT + "." + tableName, params);
			log.info("select table '{}' / count : {}", tableName, dataList.size());
			tableItem.put(putTableName, dataList);
		}
		return tableItem;
	}

	public int resultUpdate(List<String> tableNameList, List<SqlQuery> queryList, Map<String, Object> params) {
		int updateCount = 0;

		for (String tableName : tableNameList) {
			try {
				int indiUpdateCount = dynamicQueryMapper.executeUpdate(queryList,
						SQLConstants.SQL_ID_UPDATE_REUSLT + "." + tableName, params);
				log.info("update table '{}' / count : {}", tableName, indiUpdateCount);
				updateCount += indiUpdateCount;
			} catch (Exception e) {
				Throwable cause = e;
				while(cause.getCause() != null) cause = cause.getCause();
				if (cause instanceof SqlNotFoundException) {
					log.warn("Nothing sql-id [{}.{}] skip update.", SQLConstants.SQL_ID_UPDATE, tableName);
				} else {
					log.error("Other error -> ", e);
				}
			}
		}
		return updateCount;
	}

}
