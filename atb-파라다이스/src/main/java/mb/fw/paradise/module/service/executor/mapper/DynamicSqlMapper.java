package mb.fw.paradise.module.service.executor.mapper;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import mb.fw.paradise.api.model.SqlQuery;

@Mapper
public interface DynamicSqlMapper {

    @SelectProvider(type = DynamicSqlProvider.class, method = "getSql")
    List<Map<String, Object>> executeSelectList(@Param("queryList") List<SqlQuery> queryList, @Param("sqlId") String sqlId, @Param("params") @Nullable Map<String, Object> params);
    
    @InsertProvider(type = DynamicSqlProvider.class, method = "getSql")
    int executeInsertList(@Param("queryList") List<SqlQuery> queryList, @Param("sqlId") String sqlId, @Param("list") List<Map<String, Object>> dataList);
    
    @DeleteProvider(type = DynamicSqlProvider.class, method = "getSql")
    int executeDelete(@Param("queryList") List<SqlQuery> queryList, @Param("sqlId") String sqlId, @Param("params") @Nullable Map<String, Object> params);
    
    @UpdateProvider(type = DynamicSqlProvider.class, method = "getSql")
    int executeUpdate(@Param("queryList") List<SqlQuery> queryList, @Param("sqlId") String sqlId, @Param("params") @Nullable Map<String, Object> params);
}

