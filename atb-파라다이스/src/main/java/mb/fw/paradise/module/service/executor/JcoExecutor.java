package mb.fw.paradise.module.service.executor;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.codec.binary.Base64;

import com.sap.conn.jco.JCoField;
import com.sap.conn.jco.JCoFieldIterator;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoTable;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.PatternProperty;
import mb.fw.paradise.constants.InterfaceInfoPropertyConstants;
import mb.fw.paradise.dto.DataItem;
import mb.fw.paradise.util.InterfaceInfoPropertyUtil;

@Slf4j
public class JcoExecutor {

	public static void importTables(LinkedHashMap<String, List<Map<String, Object>>> tableItem, JCoParameterList tableParamList, List<PatternProperty> propertyList) {
		// JCoTable인 경우 처리
		if (tableParamList != null) {
			JCoFieldIterator tableJcoFieldIterator = tableParamList.getFieldIterator();
			
			// table-name mapping
			if (InterfaceInfoPropertyUtil.existProperty(propertyList, InterfaceInfoPropertyConstants.RFC_TABLE_MAPPINGS)) {
				String mappingStr = InterfaceInfoPropertyUtil.getValue(propertyList, InterfaceInfoPropertyConstants.RFC_TABLE_MAPPINGS);
				Map<String, String> mappingMap = Arrays.stream(mappingStr.split(",")).map(pair -> pair.split(":", 2))
						.collect(Collectors.toMap(arr -> arr[0].trim(), arr -> arr[1].trim()));
				mappingMap.forEach((beforeTableName, afterTableName) -> {
					if (tableItem.containsKey(beforeTableName)) {
						List<Map<String, Object>> valueList = tableItem.remove(beforeTableName); // 기존 key 제거하면서 value 추출
						tableItem.put(afterTableName, valueList); 
					}

				});
			}
			
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

	public static void importParms(LinkedHashMap<String, Object> parameter, JCoParameterList paramList,
			List<PatternProperty> propertyList) {
		if (paramList != null) {
			JCoFieldIterator jcoFieldIterator = paramList.getFieldIterator();

			// parameter mapping
			if (InterfaceInfoPropertyUtil.existProperty(propertyList, InterfaceInfoPropertyConstants.RFC_PARAMETER_MAPPINGS)) {
				String mappingStr = InterfaceInfoPropertyUtil.getValue(propertyList, InterfaceInfoPropertyConstants.RFC_PARAMETER_MAPPINGS);
				Map<String, String> mappingMap = Arrays.stream(mappingStr.split(",")).map(pair -> pair.split(":", 2))
						.collect(Collectors.toMap(arr -> arr[0].trim(), arr -> arr[1].trim()));
				mappingMap.forEach((beforeKey, afterKey) -> {
					if (parameter.containsKey(beforeKey)) {
						Object valueObject = parameter.remove(beforeKey); // 기존 key 제거하면서 value 추출
						parameter.put(afterKey, valueObject); //
					}

				});
			}

			while (jcoFieldIterator.hasNextField()) {
				JCoField jcoField = jcoFieldIterator.nextField();

				if ("STRUCTURE".equals(jcoField.getTypeAsString())) {
//					JCoStructure jcoStructure = jcoField.getStructure();
//					for(int i=0;i<structureArray.length();i++){
//						JSONObject structureJsonObj = structureArray.getJSONObject(i);
//						JSONArray structureNames = structureJsonObj.names();
//						for(Object structureNameObj : structureNames){
//							String structureName = (String)structureNameObj;
//							JSONObject structureObj = structureJsonObj.getJSONObject(structureName);
//							JSONArray structureColNames = structureObj.names();
//							for(Object structureColNameObj : structureColNames){
//								String structureColName = (String)structureColNameObj;
//								String structureColValue = Util.getJsonValueAsString(structureObj, structureColName);
//								
//								if(jcoField.getName().equalsIgnoreCase(structureName) && checkHasField(jcoStructure, structureColName)){
//									jcoStructure.setValue(structureColName, structureColValue);
//								}
//							}
//						}
//					}
					// JCoField인 경우 처리
				} else {
					parameter.forEach((key, value) -> {
						if (jcoField.getName().equalsIgnoreCase(key)) {
							if (key.equalsIgnoreCase("I_DATA")) {
								log.debug("decode I_DATA.");
								byte[] decoded = Base64.decodeBase64(value.toString());
								jcoField.setValue(decoded);
							} else {
								jcoField.setValue(value);
							}
						}
					});
				}
			}
		}
	}

	public static DataItem exportData(JCoParameterList exportParamList, JCoParameterList tableParamList,
			List<String> exportTableList) {
		LinkedHashMap<String, Object> resultParamMap = new LinkedHashMap<>();
		int fieldCnt = exportParamList.getFieldCount();
		for (int i = 0; i < fieldCnt; i++)
			resultParamMap.put(exportParamList.getMetaData().getName(i), exportParamList.getValue(i));

		LinkedHashMap<String, List<Map<String, Object>>> resultTableMap = exportTableList.stream()
				.collect(Collectors.toMap(tableName -> tableName,
						tableName -> convertJCoTable(tableParamList.getTable(tableName)), (v1, v2) -> v1,
						LinkedHashMap::new));

		return DataItem.builder().param(resultParamMap).table(resultTableMap).build();
	}

	private static List<Map<String, Object>> convertJCoTable(JCoTable table) {
		return IntStream.range(0, table.getNumRows()).mapToObj(i -> {
			table.setRow(i);
			Map<String, Object> row = new LinkedHashMap<>();
			JCoFieldIterator it = table.getFieldIterator();
			while (it.hasNextField()) {
				JCoField field = it.nextField();
				row.put(field.getName(), field.getValue());
			}
			return row;
		}).collect(Collectors.toList());
	}
}

