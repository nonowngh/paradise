package mb.fw.paradise.config;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.AdaptorConstants;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "adaptor.module-config", ignoreUnknownFields = true)
public class ModuleConfig {

	// 배치 스케줄 실행 task 클래스
	private String batchTask;
	// 해당 어댑터 등록 인터페이스 리스트
	private List<String> interfaceList;
	// 해당 어댑터 시스템 코드
	private String systemCode;
	// 대량 처리건수
	private int largeDataChunkSize = 1000;
	// 배치 인터페이스 크론 리로딩 스케줄러 설정
	private int schedulerRefreshIntervalSeconds = 0;

	@Autowired
	private Environment env;

	@PostConstruct
	public void init() {
		String adaptorType = env.getProperty("adaptor.type");
		if (adaptorType != null)
			log.info("adaptor-type : [{}]", adaptorType);
		if (batchTask != null && !batchTask.isEmpty())
			log.info("Setting property batch-task : {}", batchTask);
		if (interfaceList != null && !interfaceList.isEmpty()) {
			log.info("Setting property interface-list : {}", interfaceList);
			log.info("Setting property large-data-chunk-size : {}", largeDataChunkSize);
		}
		if (systemCode != null && !systemCode.isEmpty()) {
			log.info("Setting property my system-code : {}", systemCode);
			AdaptorConstants.MY_SYSTEM_CODE = systemCode;
		}
	}

}
