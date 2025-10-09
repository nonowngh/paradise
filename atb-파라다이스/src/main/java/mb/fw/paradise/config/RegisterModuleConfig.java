package mb.fw.paradise.config;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.config.prop.ModuleProp;
import mb.fw.paradise.constants.AdaptorConstants;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "adaptor", ignoreUnknownFields = true)
public class RegisterModuleConfig {

	private ModuleProp moduleProp;
	
	@PostConstruct
	public void init() {
		if(moduleProp == null) return;
		String batchTask = moduleProp.getBatchTask();
		if (batchTask != null && !batchTask.isEmpty())
			log.info("Setting property batch-task : {}", batchTask);
		List<String> interfaceList = moduleProp.getInterfaceList();
		if (interfaceList != null && !interfaceList.isEmpty())
			log.info("Setting property interface-list : {}", interfaceList);
		String mySystemCode = moduleProp.getSystemCode();
		if (!mySystemCode.isEmpty()) {
			log.info("Setting property my system-code : {}", mySystemCode);
			AdaptorConstants.MY_SYSTEM_CODE = mySystemCode;
		}
	}
	
}
