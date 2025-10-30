package mb.fw.paradise.module.service.executor.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.module.service.executor.exception.SqlNotFoundException;

@Slf4j
public class DynamicSqlProvider {

	@SuppressWarnings("unchecked")
	public String getSql(Map<String, Object> param) {
		String sqlId = (String) param.get("sqlId");
		List<SqlQuery> queryList = (List<SqlQuery>) param.get("queryList");
		Map<String, Object> params = (Map<String, Object>) param.get("params");

		log.debug("query excute parameter -> [{}]", params);
		// params null 체크
		if (params == null) params = Collections.emptyMap();

		param.putAll(params);

		return queryList.stream().filter(q -> sqlId.equals(q.getSqlId())).findFirst().map(SqlQuery::getQuery)
				.orElseThrow(() -> new SqlNotFoundException(sqlId));
	}
}