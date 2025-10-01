package mb.fw.paradise.module.service.sqlexecutor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.SqlQuery;
import mb.fw.paradise.constants.SQLConstants;
import mb.fw.paradise.dto.DataItem.Table;

@Slf4j
@Service
public class SendQueryExecutor {

	@Qualifier("simpleSqlSessionTemplate")
	private final SqlSessionTemplate simpleSqlSessionTemplate;

	public SendQueryExecutor(SqlSessionTemplate simpleSqlSessionTemplate) {
		this.simpleSqlSessionTemplate = simpleSqlSessionTemplate;
	}

	public int update(List<String> tableNameList, List<SqlQuery> queryList, Map<String, Object> params) {
		int updateCount = 0;
		for (String tableName : tableNameList) {
			Optional<SqlQuery> result = queryList.stream()
					.filter(q -> (SQLConstants.SQL_ID_UPDATE + "." + tableName).equals(q.getSqlId())).findFirst();
			if (result.isPresent()) {
				updateCount += simpleSqlSessionTemplate.update(result.get().getQuery(), params);
			} else
				log.info("Nothing sql-id [{}.{}]", SQLConstants.SQL_ID_UPDATE, tableName);
		}
		return updateCount;
	}

	public Table getTableData(List<String> sendTableNameList, List<String> recvTableNameList, List<SqlQuery> queryList,
			Map<String, Object> params) {
		// 데이터 조회
		LinkedHashMap<String, List<Map<String, Object>>> tableItem = new LinkedHashMap<>();
		for (String tableName : sendTableNameList) {
			int index = sendTableNameList.indexOf(tableName);
			if (index < 0 || index >= recvTableNameList.size()) {
				continue; // 해당 테이블에 대한 수신 테이블명이 없으면 skip
			}
			Optional<SqlQuery> result = queryList.stream()
					.filter(q -> (SQLConstants.SQL_ID_SELECT + "." + tableName).equals(q.getSqlId())).findFirst();
			if (result.isPresent()) {
				tableItem.put(recvTableNameList.get(index),
						simpleSqlSessionTemplate.selectList(result.get().getQuery(), params));
			}
		}
		return Table.builder().tableItem(tableItem).build();
	}

	public int resultUpdate(List<String> tableNameList, List<SqlQuery> queryList, Map<String, Object> params) {
		int updateCount = 0;

		for (String tableName : tableNameList) {
			Optional<SqlQuery> result = queryList.stream()
					.filter(q -> (SQLConstants.SQL_ID_UPDATE_REUSLT + "." + tableName).equals(q.getSqlId()))
					.findFirst();
			if (result.isPresent()) {
				updateCount += simpleSqlSessionTemplate.update(result.get().getQuery(), params);
			} else
				log.info("Nothing sql-id [{}.{}]", SQLConstants.SQL_ID_UPDATE_REUSLT, tableName);
		}
		return updateCount;
	}

}
