package mb.fw.paradise.module.metaapi.model;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 기본 생성자 (Jackson 역직렬화 및 JPA용)
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자 (new PatternProperty(a, b)용)
public class SqlQuery {

	private String sqlId;
	
	private String query;
	
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SqlQuery)) return false;
        SqlQuery that = (SqlQuery) o;
        return Objects.equals(sqlId, that.sqlId)
            && Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlId, query);
    }
}
