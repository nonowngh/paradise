package mb.fw.paradise.module.metaapi.model;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 기본 생성자 (Jackson 역직렬화 및 JPA용)
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자 (new PatternProperty(a, b)용)
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
