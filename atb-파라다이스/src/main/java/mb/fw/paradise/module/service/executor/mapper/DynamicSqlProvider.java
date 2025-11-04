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

		if (param.containsKey("params")) {
			Map<String, Object> params = (Map<String, Object>) param.get("params");
			// params null 체크
			if (params == null)
				params = Collections.emptyMap();
			param.putAll(params);
			log.debug("query excute parameter -> [{}]", params);
		}

		return queryList.stream().filter(q -> sqlId.equals(q.getSqlId())).findFirst().map(SqlQuery::getQuery)
				.orElseThrow(() -> new SqlNotFoundException(sqlId));
	}

//	@SuppressWarnings("unchecked")
//	public String getSqlDataList(Map<String, Object> param) {
//		String sqlId = (String) param.get("sqlId");
//		List<SqlQuery> queryList = (List<SqlQuery>) param.get("queryList");
//
//		SqlQuery sqlQuery = queryList.stream().filter(q -> sqlId.equals(q.getSqlId())).findFirst()
//				.orElseThrow(() -> new SqlNotFoundException("SQL not found for id: " + sqlId));
//
//		// 이미 완성된 SQL을 수정하지 않고, MyBatis foreach로 감싼다.
//		// #{id}, #{name} 같은 단건 파라미터도 foreach 안에서는 #{item.id}, #{item.name}으로 바인딩됨.
//		String wrappedSql = "<script>" + "<foreach collection='list' item='item' separator=';'>" + sqlQuery.getQuery()
//				+ "</foreach>" + "</script>";
//
//		log.debug("Generated dynamic SQL for [{}]: \n{}", sqlId, wrappedSql);
//		return wrappedSql;
//	}
}