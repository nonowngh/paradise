package mb.fw.paradise.api.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import mb.fw.paradise.api.model.InterfaceInfo;

@Mapper
public interface InterfaceInfoMapper {

    InterfaceInfo selectInterfaceWithDetails(@Param("interfaceId") String interfaceId);
	
	List<InterfaceInfo> selectInterfaceCronExpressionList(List<String> interfaceIdList);
}
