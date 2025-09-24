package mb.fw.paradise.api.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.api.service.InterfaceInfoService;
import mb.fw.paradise.constants.APIContextPathConstants;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Profile("api")
@RestController
public class InterfaceInfoController {

	private final InterfaceInfoService interfaceInfoService;

	public InterfaceInfoController(InterfaceInfoService interfaceInfoService) {
		this.interfaceInfoService = interfaceInfoService;
	}

	@GetMapping(APIContextPathConstants.INTERFACE_INFO_API + "/{interfaceId}")
	public Mono<ResponseEntity<InterfaceInfo>> getInterfaceInfo(@PathVariable String interfaceId) {
		return interfaceInfoService.getInterfaceInfoByInterfaceId(interfaceId).map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@GetMapping(APIContextPathConstants.INTERFACE_INFO_API + APIContextPathConstants.INTERFACE_INFO_API_CLEAR_CACHE)
	public Mono<ResponseEntity<String>> clearCacheInterfaceInfo() {
		return Mono.fromRunnable(() -> interfaceInfoService.clearAllInfoCache())
				.subscribeOn(Schedulers.boundedElastic()).thenReturn(ResponseEntity.ok("Cache successfully cleared"));
	}

	@PostMapping(APIContextPathConstants.INTERFACE_INFO_API + APIContextPathConstants.INTERFACE_INFO_API_SCHEDULE_LIST)
	public Mono<List<InterfaceInfo>> getInterfaceScheduleList(@RequestBody List<String> interfaceIdList) {
		return interfaceInfoService.getScheduleList(interfaceIdList);
	}
}
