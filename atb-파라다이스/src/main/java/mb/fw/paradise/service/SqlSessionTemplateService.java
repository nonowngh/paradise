package mb.fw.paradise.service;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;

import mb.fw.paradise.api.model.SqlQuery;

@Service
public class SqlSessionTemplateService {
	
	private final SqlSessionTemplate sqlSessionTemplate;

	public SqlSessionTemplateService(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}
	
	public int update(Map<String, Object> params, SqlQuery sqlQuery) {
		return sqlSessionTemplate.update(sqlQuery.getQuery(), params);
	}
	
	public List<Map<String, Object>> selectList(Map<String, Object> params, SqlQuery sqlQuery) {
		return sqlSessionTemplate.selectList(sqlQuery.getQuery(), params);
	}
}
