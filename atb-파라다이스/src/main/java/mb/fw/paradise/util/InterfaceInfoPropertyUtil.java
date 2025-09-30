package mb.fw.paradise.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import mb.fw.paradise.api.model.PatternProperty;

public class InterfaceInfoPropertyUtil {

	public static List<String> getValueList(List<PatternProperty> propertyList, String propertyName) {
		return propertyList.stream().filter(property -> propertyName.equals(property.getPropertyName()))
				.flatMap(p -> Arrays.stream(p.getPropertyValue().split(","))).map(String::trim)
				.filter(s -> !s.isEmpty()).collect(Collectors.toList());
	}

	public static String getValue(List<PatternProperty> propertyList, String propertyName) {
		return propertyList.stream().filter(p -> propertyName.equals(p.getPropertyName()))
				.map(PatternProperty::getPropertyValue).findFirst().orElse(null);
	}
}
