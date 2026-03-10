package mb.fw.paradise.module.metaapi.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.module.metaapi.entity.MetaInfoEntity;
import mb.fw.paradise.module.metaapi.model.MetaApiModel;
import mb.fw.paradise.module.metaapi.model.PatternProperty;
import mb.fw.paradise.module.metaapi.model.SqlQuery;
import mb.fw.paradise.module.metaapi.repository.MetaApiRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaApiService {

	private final MetaApiRepository metaApiRepository;

	/**
	 * [단일 조회] 상세 정보 포함
	 */
	@Transactional(readOnly = true)
	@Cacheable(value = "MetaInfoCache", key = "#id")
	public MetaApiModel getInterfaceInfoByInterfaceId(String id) {
		return metaApiRepository.findWithDetailsByInterfaceId(id).map(this::convertToModel).orElse(null);
	}

	/**
	 * [기능별 조회] RFC Function 등 조건부 리스트 조회
	 */
	@Transactional(readOnly = true)
	public List<MetaApiModel> getInterfaceInfoListByFunctionName(List<String> interfaceIdList, String functionName) {
		return metaApiRepository.findWithDetailsByFunctionName(interfaceIdList, functionName).stream()
				.map(this::convertToModel).collect(Collectors.toList());
	}

	/**
	 * [스케줄러용] WebClient.block() 호출에 대응하는 데이터 반환
	 */
	@Transactional(readOnly = true)
	public List<MetaApiModel> getScheduleList(List<String> interfaceIdList) {
		// 클라이언트에서 전체 모델을 원하므로 findByInterfaceIdIn 사용
		return metaApiRepository.findByInterfaceIdIn(interfaceIdList).stream().map(this::convertToModel)
				.collect(Collectors.toList());
	}

	/**
	 * 캐시 삭제
	 */
	@CacheEvict(value = "MetaInfoCache", allEntries = true)
	public void clearAllInfoCache() {
		log.info("MetaInfoCache cleared.");
	}

	// --- 변환 로직 (Entity -> Model) ---

	private MetaApiModel convertToModel(MetaInfoEntity entity) {
		MetaApiModel model = new MetaApiModel();
		model.setInterfaceId(entity.getInterfaceId());
		model.setCronExpression(entity.getCronExpression());
		model.setPatternType(entity.getPatternType());
		model.setSndSystemCode(entity.getSendSystemCode());
		model.setRcvSystemCode(entity.getRecvSystemCode());

		// Property 변환
		if (entity.getPropertyList() != null) {
			model.setPropertyList(entity.getPropertyList().stream()
					.map(p -> new PatternProperty(p.getPropertyName(), p.getPropertyValue()))
					.collect(Collectors.toSet()));
		}

		// SQL Query 변환
		if (entity.getSqlQueryList() != null) {
			model.setSqlQueryList(entity.getSqlQueryList().stream().map(q -> new SqlQuery(q.getSqlId(), q.getQuery()))
					.collect(Collectors.toSet()));
		}

		return model;
	}
}