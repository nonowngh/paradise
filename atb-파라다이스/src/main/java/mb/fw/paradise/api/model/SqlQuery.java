package mb.fw.paradise.api.model;

import java.util.Objects;

import lombok.Data;

@Data
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
