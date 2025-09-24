package mb.fw.paradise.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class DataItem {

	private Table table;

	private Structure structure;

	private Parameter parameter;

	class Table {
		LinkedHashMap<String, List<Map<String, Object>>> tableItem;
	}

	class Structure {
		LinkedHashMap<String, Map<String, Object>> structureItem;

	}

	class Parameter {
		LinkedHashMap<String, Object> parameterItem;
	}
}
