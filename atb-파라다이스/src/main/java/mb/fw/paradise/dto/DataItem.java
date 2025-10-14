package mb.fw.paradise.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataItem {

	LinkedHashMap<String, List<Map<String, Object>>> table;

	LinkedHashMap<String, Map<String, Object>> structure;

	LinkedHashMap<String, Object> parameter;

//	@Data
//	@Builder
//	public static class Table {
//		LinkedHashMap<String, List<Map<String, Object>>> tableItem;
//	}
//
//	@Data
//	public static  class Structure {
//		LinkedHashMap<String, Map<String, Object>> structureItem;
//	}
//
//	@Data
//	public static class Parameter {
//		LinkedHashMap<String, Object> parameterItem;
//	}
}
