package mb.fw.paradise.config;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorConstants;
import mb.fw.paradise.constants.AdaptorType;

@Slf4j
@Data
@Configuration
@ConditionalOnAdaptorType(value = { AdaptorType.INTERFACE_API, AdaptorType.GATEWAY }, negate = true)
@ConfigurationProperties(prefix = "adaptor.module-config", ignoreUnknownFields = true)
public class ModuleConfig {

	// 배치 스케줄 실행 task 클래스
	private String batchTask;
	// 해당 어댑터 등록 인터페이스 리스트
	private List<String> interfaceList;
	// 해당 어댑터 시스템 코드
	private String systemCode;

	@PostConstruct
	public void init() {
		if (batchTask != null && !batchTask.isEmpty())
			log.info("Setting property batch-task : {}", batchTask);
		if (interfaceList != null && !interfaceList.isEmpty())
			log.info("Setting property interface-list : {}", interfaceList);
		if (!systemCode.isEmpty()) {
			log.info("Setting property my system-code : {}", systemCode);
			AdaptorConstants.MY_SYSTEM_CODE = systemCode;
		}
	}

}
