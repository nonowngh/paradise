package mb.fw.paradise.api.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.mapper.InterfaceInfoMapper;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@ConditionalOnAdaptorType(AdaptorType.INTERFACE_API)
public class InterfaceInfoService {
	private final InterfaceInfoMapper interfaceInfoMapper;

	public InterfaceInfoService(InterfaceInfoMapper interfaceInfoMapper) {
		this.interfaceInfoMapper = interfaceInfoMapper;
	}

	@Cacheable(value = "interfaceInfoCache", key = "#id")
	public InterfaceInfo getInterfaceInfoByInterfaceId(String id) {
	    return interfaceInfoMapper.selectInterfaceWithDetails(id);
	}

	@CacheEvict(value = "interfaceInfoCache", allEntries = true)
	public void clearAllInfoCache() {
        log.info("interfaceInfoCache 캐시 삭제 완료.");
	}
	
	public Mono<List<InterfaceInfo>> getScheduleList(List<String> interfaceIdList) {
        return Mono.just(interfaceInfoMapper.selectInterfaceCronExpressionList(interfaceIdList));
    }
}
