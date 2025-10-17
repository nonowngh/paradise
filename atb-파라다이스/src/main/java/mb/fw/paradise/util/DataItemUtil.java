package mb.fw.paradise.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mb.fw.paradise.dto.DataItem;

public class DataItemUtil {

	public static int tableDataCount(DataItem resultItem) {
		if (resultItem == null || resultItem.getTable() == null) {
			return 1;
		}
		LinkedHashMap<String, List<Map<String, Object>>> table = resultItem.getTable();
		int count = table.values().stream().filter(Objects::nonNull).mapToInt(List::size).sum();
		return count == 0 ? 1 : count;
	}

	public static void main(String[] args) {
		LinkedHashMap<String, List<Map<String, Object>>> table = new LinkedHashMap<>();
		System.out.println(tableDataCount(DataItem.builder().table(table).build()));
	}
}
