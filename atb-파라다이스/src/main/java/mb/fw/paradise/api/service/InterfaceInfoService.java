package mb.fw.paradise.api.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.mapper.InterfaceInfoMapper;
import mb.fw.paradise.api.model.InterfaceInfo;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Profile("api")
@Slf4j
@Service
public class InterfaceInfoService {
	private final InterfaceInfoMapper interfaceInfoMapper;
	private final Scheduler jdbcScheduler = Schedulers.boundedElastic(); // Blocking I/O용

	public InterfaceInfoService(InterfaceInfoMapper interfaceInfoMapper) {
		this.interfaceInfoMapper = interfaceInfoMapper;
	}

	@Cacheable(value = "interfaceInfoCache", key = "#interfaceId")
	public Mono<InterfaceInfo> getInterfaceInfoByInterfaceId(String interfaceId) {
		return Mono.fromCallable(() -> interfaceInfoMapper.selectInterfaceInfoByInterfaceId(interfaceId))
				.subscribeOn(jdbcScheduler);
	}

	@CacheEvict(value = "interfaceInfoCache", allEntries = true)
	public void clearAllInfoCache() {
        log.info("interfaceInfoCache 캐시 삭제 완료.");
	}
}
