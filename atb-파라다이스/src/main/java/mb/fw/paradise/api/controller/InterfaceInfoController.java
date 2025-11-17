package mb.fw.paradise.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.api.service.InterfaceInfoService;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.ApiContextPathConstants;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@ConditionalOnAdaptorType(AdaptorType.INTERFACE_API)
@RequestMapping(ApiContextPathConstants.INTERFACE_INFO_API)
public class InterfaceInfoController {

	private final InterfaceInfoService interfaceInfoService;

	public InterfaceInfoController(InterfaceInfoService interfaceInfoService) {
		this.interfaceInfoService = interfaceInfoService;
	}

	@GetMapping("")
	public Mono<ResponseEntity<?>> getInterfaceInfo(@RequestParam String interfaceId) {
		return Mono.fromSupplier(() -> {
			InterfaceInfo info = interfaceInfoService.getInterfaceInfoByInterfaceId(interfaceId);
			return (info != null) ? ResponseEntity.ok(info) : ResponseEntity.notFound().build();
		}).onErrorResume(e -> {
			log.error("Error occurred while fetching interface info", e);
			return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()));
		});
	}

	@GetMapping(ApiContextPathConstants.INTERFACE_INFO_API_CLEAR_CACHE)
	public Mono<ResponseEntity<String>> clearCacheInterfaceInfo() {
		return Mono.fromRunnable(() -> interfaceInfoService.clearAllInfoCache())
				.subscribeOn(Schedulers.boundedElastic()).thenReturn(ResponseEntity.ok("Cache successfully cleared"));
	}

	@PostMapping(ApiContextPathConstants.INTERFACE_INFO_API_SCHEDULE_LIST)
	public Mono<List<InterfaceInfo>> getInterfaceScheduleList(@RequestBody List<String> interfaceIdList) {
		return interfaceInfoService.getScheduleList(interfaceIdList);
	}

	@PostMapping(ApiContextPathConstants.INTERFACE_INFO_API_LIST_RFC_FUNCTION)
	public Mono<ResponseEntity<? extends Object>> getInterfaceInfoListForRfcFunction(
			@RequestBody List<String> interfaceIdList, @RequestParam(required = false) String rfcFunctionName) {
		return Mono.fromSupplier(() -> {
			List<InterfaceInfo> infoList = interfaceInfoService
					.getInterfaceInfoListByInterfaceIdAndFunctionName(interfaceIdList, rfcFunctionName);
			return (infoList != null && infoList.size() > 0) ? ResponseEntity.ok(infoList)
					: ResponseEntity.notFound().build();
		}).onErrorResume(e -> {
			log.error("Error occurred while fetching interface info", e);
			return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()));
		});
	}
}
