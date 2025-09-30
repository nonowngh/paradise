package mb.fw.paradise.api.model;

import java.util.Objects;

import lombok.Data;

@Data
public class PatternProperty {

	String propertyName;
	
	String propertyValue;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof PatternProperty))
			return false;
		PatternProperty that = (PatternProperty) o;
		return Objects.equals(propertyName, that.propertyName) && Objects.equals(propertyValue, that.propertyValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(propertyName, propertyValue);
	}

}
