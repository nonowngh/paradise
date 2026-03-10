package mb.fw.paradise.common.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import mb.fw.paradise.module.metaapi.model.PatternProperty;

public class InterfaceInfoPropertyUtil {

	public static List<String> getValueList(List<PatternProperty> propertyList, String propertyName) {
//		return propertyList.stream().filter(property -> propertyName.equals(property.getPropertyName()))
//				.flatMap(p -> Arrays.stream(p.getPropertyValue().split(","))).map(String::trim)
//				.filter(s -> !s.isEmpty()).collect(Collectors.toList());
		List<String> values = propertyList.stream().filter(property -> propertyName.equals(property.getPropertyName()))
				.flatMap(p -> Arrays.stream(p.getPropertyValue().split(","))).map(String::trim)
				.filter(s -> !s.isEmpty()).collect(Collectors.toList());

		if (values.isEmpty()) {
			throw new IllegalArgumentException("No matching values found for property: " + propertyName);
		}
		return values;
	}

	public static Map<String, Object> getValueMap(List<PatternProperty> propertyList, String propertyName) {
		return propertyList.stream().filter(property -> propertyName.equals(property.getPropertyName()))
				.flatMap(property -> Arrays.stream(property.getPropertyValue().split(","))).map(String::trim)
				.filter(s -> !s.isEmpty() && s.contains(":")).map(s -> s.split(":", 2))
				.collect(Collectors.toMap(arr -> arr[0], arr -> arr[1], (v1, v2) -> v2, LinkedHashMap::new));
	}

	public static String getValue(List<PatternProperty> propertyList, String propertyName) {
		return propertyList.stream().filter(p -> propertyName.equals(p.getPropertyName()))
				.map(PatternProperty::getPropertyValue).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No value found for property: " + propertyName));
	}

	public static boolean existProperty(List<PatternProperty> propertyList, String propertyName) {
		return propertyList.stream().filter(p -> propertyName.equals(p.getPropertyName()))
				.map(PatternProperty::getPropertyValue).findAny().isPresent();
	}
}
