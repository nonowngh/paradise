package mb.fw.paradise.module.service;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.config.MyBatisConfig;
import mb.fw.paradise.constants.SQLConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.DataItem.Table;

@Service
@ConditionalOnBean(DataSource.class)
public class InsertQueryExecutor {

	@Qualifier("simpleSqlSessionTemplate")
	private final SqlSessionTemplate simpleSqlSessionTemplate;
	@Qualifier("batchSqlSessionTemplate")
	private final SqlSessionTemplate batchSqlSessionTemplate;

	private final MyBatisConfig config;

	public InsertQueryExecutor(SqlSessionTemplate simpleSqlSessionTemplate, SqlSessionTemplate batchSqlSessionTemplate,
			MyBatisConfig config) {
		this.simpleSqlSessionTemplate = simpleSqlSessionTemplate;
		this.batchSqlSessionTemplate = batchSqlSessionTemplate;
		this.config = config;
	}

	public void processInsertQueries(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		List<SqlQuery> queryList = interfaceInfo.getSqlQueryList();
		Table tableData = request.getDataItem().getTable();
		tableData.getTableItem().forEach((tableName, data) -> {
			String expectedSqlId = SQLConstants.SQL_ID_INSERT + "." + tableName;

			queryList.stream().filter(q -> expectedSqlId.equals(q.getSqlId())).findFirst().ifPresent(query -> {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) data;
				if (dataList.size() >= config.getThresholdCount()) {
					insertBatch(query.getQuery(), dataList);
				} else {
					insertSimple(query.getQuery(), dataList);
				}
			});
		});
	}

	private void insertSimple(String queryId, List<Map<String, Object>> dataList) {
		dataList.forEach(row -> simpleSqlSessionTemplate.insert(queryId, row));
	}

	private void insertBatch(String queryId, List<Map<String, Object>> dataList) {
		SqlSession batchSession = batchSqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false); // 수동
		try {
			dataList.forEach(row -> batchSession.insert(queryId, row));
			batchSession.commit(); // ✅ 수동 커밋
		} catch (Exception e) {
			batchSession.rollback(); // ✅ 예외 시 rollback
			throw new RuntimeException("Batch insert failed", e);
		} finally {
			batchSession.close(); // ✅ 세션 종료
		}
	}
}
