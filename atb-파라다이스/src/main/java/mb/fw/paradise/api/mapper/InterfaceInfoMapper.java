package mb.fw.paradise.api.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import mb.fw.paradise.api.model.InterfaceInfo;

@Mapper
public interface InterfaceInfoMapper {

	@Select("SELECT * FROM interface-info WHERE interfaceId = #{interfaceId}")
	InterfaceInfo selectInterfaceInfoByInterfaceId(String interfaceId);
	
	@Select("SELECT interfaceId, cron-expression FROM interface-info WHERE interfaceId IN (#{interfaceIdList})")
	List<InterfaceInfo> selectInterfaceCronExpressionListByInterfaceIdList(List<String> interfaceIdList);
}
