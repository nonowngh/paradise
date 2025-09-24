package mb.fw.paradise.api.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import mb.fw.paradise.api.model.InterfaceInfo;

@Mapper
public interface InterfaceInfoMapper {

	InterfaceInfo selectInterfaceInfoByInterfaceId(String interfaceId);
	
	List<InterfaceInfo> selectInterfaceCronExpressionListByInterfaceIdList(List<String> interfaceIdList);
}
