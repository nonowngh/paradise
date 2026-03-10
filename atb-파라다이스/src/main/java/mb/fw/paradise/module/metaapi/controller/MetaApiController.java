package mb.fw.paradise.module.metaapi.controller;

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
import mb.fw.paradise.common.constants.ApiContextPathConstants;
import mb.fw.paradise.module.metaapi.model.MetaApiModel;
import mb.fw.paradise.module.metaapi.service.MetaApiService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequestMapping(ApiContextPathConstants.META_API)
public class MetaApiController {

    private final MetaApiService metaApiService;

    public MetaApiController(MetaApiService metaApiService) {
        this.metaApiService = metaApiService;
    }

    /**
     * [상세 조회] 특정 인터페이스 정보 조회 (Entity -> Model 변환 반영)
     */
    @GetMapping("")
    public Mono<ResponseEntity<MetaApiModel>> getInterfaceInfo(@RequestParam String interfaceId) {
        return Mono.fromSupplier(() -> metaApiService.getInterfaceInfoByInterfaceId(interfaceId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(info -> {
                    if (info == null) {
                        return new ResponseEntity<MetaApiModel>(HttpStatus.NOT_FOUND);
                    }
                    return new ResponseEntity<>(info, HttpStatus.OK);
                })
                .doOnError(e -> log.error("인터페이스 정보 조회 중 오류 발생: {}", interfaceId, e))
                .onErrorResume(e -> Mono.just(new ResponseEntity<MetaApiModel>(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    /**
     * [캐시 삭제] 전체 메타정보 캐시 강제 초기화
     */
    @GetMapping(ApiContextPathConstants.META_API_CLEAR_CACHE)
    public Mono<ResponseEntity<String>> clearCacheInterfaceInfo() {
        return Mono.fromRunnable(metaApiService::clearAllInfoCache)
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(new ResponseEntity<>("Cache successfully cleared", HttpStatus.OK));
    }

    /**
     * [스케줄러용] 클라이언트의 .block() 호출에 대응하는 리스트 조회
     * 클라이언트 요구사항에 맞춰 MetaApiModel 리스트를 반환합니다.
     */
    @PostMapping(ApiContextPathConstants.META_API_SCHEDULE_LIST)
    public Mono<ResponseEntity<List<MetaApiModel>>> getInterfaceScheduleList(@RequestBody List<String> interfaceIdList) {
        return Mono.fromSupplier(() -> metaApiService.getScheduleList(interfaceIdList))
                .subscribeOn(Schedulers.boundedElastic())
                .map(list -> {
                    if (list == null || list.isEmpty()) {
                        return new ResponseEntity<List<MetaApiModel>>(HttpStatus.NO_CONTENT);
                    }
                    return new ResponseEntity<>(list, HttpStatus.OK);
                })
                .doOnError(e -> log.error("스케줄 리스트 조회 중 오류 발생", e))
                .onErrorResume(e -> Mono.just(new ResponseEntity<List<MetaApiModel>>(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    /**
     * [기능별 조회] RFC Function 명칭 기준 상세 정보 리스트 조회
     */
    @PostMapping(ApiContextPathConstants.META_API_LIST_RFC_FUNCTION)
    public Mono<ResponseEntity<List<MetaApiModel>>> getInterfaceInfoListForRfcFunction(
            @RequestBody List<String> interfaceIdList, 
            @RequestParam(required = false) String rfcFunctionName) {
        
        return Mono.fromSupplier(() -> metaApiService.getInterfaceInfoListByFunctionName(interfaceIdList, rfcFunctionName))
                .subscribeOn(Schedulers.boundedElastic())
                .map(infoList -> {
                    if (infoList == null || infoList.isEmpty()) {
                        return new ResponseEntity<List<MetaApiModel>>(HttpStatus.NOT_FOUND);
                    }
                    return new ResponseEntity<>(infoList, HttpStatus.OK);
                })
                .doOnError(e -> log.error("RFC 기반 인터페이스 리스트 조회 중 오류 발생", e))
                .onErrorResume(e -> Mono.just(new ResponseEntity<List<MetaApiModel>>(HttpStatus.INTERNAL_SERVER_ERROR)));
    }
}