package mb.fw.paradise.module.service.sqlexecutor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
import mb.fw.paradise.util.InterfaceInfoPropertyUtil;

@Service
@ConditionalOnAdaptorType(AdaptorType.DB)
public class ReceiveQueryExecutor {

	@Qualifier("simpleSqlSessionTemplate")
	private final SqlSessionTemplate simpleSqlSessionTemplate;
	@Qualifier("batchSqlSessionTemplate")
	private final SqlSessionTemplate batchSqlSessionTemplate;

	private final MyBatisConfig config;

	public ReceiveQueryExecutor(SqlSessionTemplate simpleSqlSessionTemplate, SqlSessionTemplate batchSqlSessionTemplate,
			MyBatisConfig config) {
		this.simpleSqlSessionTemplate = simpleSqlSessionTemplate;
		this.batchSqlSessionTemplate = batchSqlSessionTemplate;
		this.config = config;
	}

	public void processDelete(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
		LinkedHashMap<String, List<Map<String, Object>>> tableData = request.getDataItem().getTable();
		tableData.forEach((tableName, data) -> {
			String expectedSqlId = SQLConstants.SQL_ID_DELETE + "." + tableName;
			queryList.stream().filter(q -> expectedSqlId.equals(q.getSqlId())).findFirst().ifPresent(query -> {
				simpleSqlSessionTemplate.delete(query.getQuery());
			});
		});
	}

	public void processInsert(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
		LinkedHashMap<String, List<Map<String, Object>>> tableData = request.getDataItem().getTable();
		tableData.forEach((tableName, data) -> {
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
	
	public DataItem processSelect(InterfaceInfo interfaceInfo, APIRequestMessage request) {
		List<SqlQuery> queryList = new ArrayList<>(interfaceInfo.getSqlQueryList());
		LinkedHashMap<String, Object> param = request.getDataItem().getParameter();
		List<PatternProperty> propertyList = new ArrayList<>(interfaceInfo.getPropertyList());
		LinkedHashMap<String, List<Map<String, Object>>> tableMap = new LinkedHashMap<>();
		if(InterfaceInfoPropertyUtil.existProperty(propertyList, InterfaceInfoPropertyConstants.RECV_TABLE_NAMES)) {
			List<String> tableNameList = InterfaceInfoPropertyUtil.getValueList(new ArrayList<>(interfaceInfo.getPropertyList()),
					InterfaceInfoPropertyConstants.RECV_TABLE_NAMES);
			tableNameList.forEach(tableName -> {
				String expectedSqlId = SQLConstants.SQL_ID_SELECT + "." + tableName;
				queryList.stream().filter(q -> expectedSqlId.equals(q.getSqlId())).findFirst().ifPresent(query -> {
					List<Map<String, Object>> dataList = simpleSqlSessionTemplate.selectList(query.getQuery(), param);
					tableMap.put(tableName, dataList);
				});
			});
		}else {
			String expectedSqlId = SQLConstants.SQL_ID_SELECT;
			queryList.stream().filter(q -> expectedSqlId.equals(q.getSqlId())).findFirst().ifPresent(query -> {
				List<Map<String, Object>> dataList = simpleSqlSessionTemplate.selectList(query.getQuery(), param);
				tableMap.put("data", dataList);
			});
		}
		return DataItem.builder().table(tableMap).build();
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
