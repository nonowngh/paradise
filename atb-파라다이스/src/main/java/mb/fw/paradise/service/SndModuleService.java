package mb.fw.paradise.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;

import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.constants.SQLConstants;
import mb.fw.paradise.dto.DataItem.Table;

@Service
public class SndModuleService {

	private final SqlSessionTemplate sqlSessionTemplate;

	public SndModuleService(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}
	
	public int update(List<String> tableNameList, List<SqlQuery> sqlQueryList, Map<String, Object> params) {
		int updateCount = 0;
		for (String tableName : tableNameList) {
			Optional<SqlQuery> result = sqlQueryList.stream()
					.filter(q -> (SQLConstants.SQL_ID_UPDATE + "." + tableName).equals(q.getSqlId())).findFirst();
			if (result.isPresent()) {
				String sql = result.get().getQuery();
				updateCount += sqlSessionTemplate.update(sql, params);
			}
		}
		return updateCount;
	}

	public Table getTableData(List<String> tableNameList, List<SqlQuery> sqlQueryList, Map<String, Object> params) {
		LinkedHashMap<String, List<Map<String, Object>>> tableItem = new LinkedHashMap<>();
		for (String tableName : tableNameList) {
			Optional<SqlQuery> result = sqlQueryList.stream()
					.filter(q -> (SQLConstants.SQL_ID_SELECT + "." + tableName).equals(q.getSqlId())).findFirst();
			if (result.isPresent()) {
				String sql = result.get().getQuery();
				tableItem.put(tableName, sqlSessionTemplate.selectList(sql, params));
			}
		}
		return Table.builder().tableItem(tableItem).build();
	}
	
	public int updateResult(List<String> tableNameList, List<SqlQuery> sqlQueryList, Map<String, Object> params) {
		int updateCount = 0;
		for (String tableName : tableNameList) {
			Optional<SqlQuery> result = sqlQueryList.stream()
					.filter(q -> (SQLConstants.SQL_ID_UPDATE_REUSLT + "." + tableName).equals(q.getSqlId())).findFirst();
			if (result.isPresent()) {
				String sql = result.get().getQuery();
				updateCount += sqlSessionTemplate.update(sql, params);
			}
		}
		return updateCount;
	}
}
