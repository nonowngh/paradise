package mb.fw.paradise.module.service.sqlexecutor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sap.conn.jco.JCoField;
import com.sap.conn.jco.JCoFieldIterator;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoTable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JcoExecutor {

	public static void importTables(LinkedHashMap<String, List<Map<String, Object>>> tableItem,
			JCoParameterList tableParamList) {
		// JCoTable인 경우 처리
		JCoFieldIterator tableJcoFieldIterator = tableParamList.getFieldIterator();
		while (tableJcoFieldIterator.hasNextField()) {
			JCoField jcoField = tableJcoFieldIterator.nextField();
			JCoTable jcoTable = jcoField.getTable();
			JCoRecordMetaData metaData = jcoTable.getRecordMetaData();
			tableItem.forEach((tableName, data) -> {
				if (jcoField.getName().equalsIgnoreCase(tableName)) {
					log.info("Import table name : {}, count : {}", tableName, data.toArray().length);
//					HashMap<String, String> replaceColumnMap = hr2SapMappingKeyMap.get(tableName); hr 맵핑??
					data.forEach(row -> {
						StringBuffer printRow = new StringBuffer();
						jcoTable.appendRow();

						row.keySet().forEach(key -> {
							for (int fieldCnt = 0; fieldCnt < metaData.getFieldCount(); fieldCnt++) {
								String fieldName = metaData.getName(fieldCnt);
								String fieldWithoutUnderBar = fieldName.replaceAll("_", "");
								String dataColumnWithoutUnderBar = key.replaceAll("_", "");
								if (fieldWithoutUnderBar.equalsIgnoreCase(dataColumnWithoutUnderBar)) {
									jcoTable.setValue(fieldName, row.get(key));
									printRow.append("[").append(fieldName).append(":").append(key).append("]");
									break;
								}
							}

						});

					});
				}
			});
		}
	}
}

//			JCoTable jcoTable = jcoField.getTable();
//			JCoRecordMetaData metaData = jcoTable.getRecordMetaData();
//			for (int i = 0; i < tableArray.length(); i++) {
//				JSONObject tableJsonObj = tableArray.getJSONObject(i);
//				JSONArray tableNames = tableJsonObj.names();
//
//				for (Object tableNameObj : tableNames) {
//					String tableName = (String) tableNameObj;
//					JSONArray tableRowArray = tableJsonObj.getJSONArray(tableName);
//
//					if (jcoField.getName().equalsIgnoreCase(tableName)) {
//						logger.info("Import table name : {}, count : {}", tableName, tableRowArray.length());
//						HashMap<String, String> replaceColumnMap = hr2SapMappingKeyMap.get(tableName);
//
//						for (int rowIdx = 0; rowIdx < tableRowArray.length(); rowIdx++) {
//							StringBuffer printRow = new StringBuffer();
//							jcoTable.appendRow();
//							JSONObject dataObj = tableRowArray.getJSONObject(rowIdx);
//
//							JSONArray dataColNames = dataObj.names();
//							for (Object dataColNameObj : dataColNames) {
//								String dataColName = (String) dataColNameObj;
//								String dataColValue = Util.getJsonValueAsString(dataObj, dataColName);
//
//								/**
//								 * hr2SapMappingKey가 설정된 경우 컬럼명을 바꿔준다. 김동명님 요구사항 2016.03.14
//								 */
//								dataColName = replaceColName(replaceColumnMap, dataColName);
//
//								/**
//								 * 그냥 필드이름으로 매핑하면 되는데, HR에서 '_' 처리가 명확하지 않아서 '_'를 제거한 string이 동일하면 넣어주는 것으로 함.
//								 * VIEW 테이블 schema와 웹서비스 client 추출 시 method 형태가 다름 ex) getEMAILADDRESS()로 뽑지만,
//								 * 테이블 schema는 email_address 임.
//								 */
//								for (int fieldCnt = 0; fieldCnt < metaData.getFieldCount(); fieldCnt++) {
//									String fieldName = metaData.getName(fieldCnt);
//									String fieldWithoutUnderBar = fieldName.replaceAll("_", "");
//									String dataColumnWithoutUnderBar = dataColName.replaceAll("_", "");
//									if (fieldWithoutUnderBar.equalsIgnoreCase(dataColumnWithoutUnderBar)) {
//										jcoTable.setValue(fieldName, dataColValue);
//										printRow.append("[").append(fieldName).append(":").append(dataColValue)
//												.append("]");
//										break;
//									}
//								}
//							}
////								logger.debug("printRow : " + printRow);
//						}
//					}
//				}
//			}
//		}
//	}
//}
