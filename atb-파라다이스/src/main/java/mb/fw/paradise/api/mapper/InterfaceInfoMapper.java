package mb.fw.paradise.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import mb.fw.paradise.api.model.InterfaceInfo;

@Mapper
public interface InterfaceInfoMapper {

	InterfaceInfo selectInterfaceInfoByInterfaceId(String interfaceId);
}
