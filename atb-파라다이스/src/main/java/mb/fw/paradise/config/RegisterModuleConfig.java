package mb.fw.paradise.config;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.config.prop.RegisterProp;
import mb.fw.paradise.constants.AdaptorConstants;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "module", ignoreUnknownFields = true)
public class RegisterModuleConfig {

	private RegisterProp registerProp;

	@PostConstruct
	public void init() {
		String batchTask = registerProp.getBatchTask();
		if (!batchTask.isEmpty())
			log.info("Setting property batch-task : {}", batchTask);
		List<String> interfaceList = registerProp.getInterfaceList();
		if (!interfaceList.isEmpty())
			log.info("Setting property interface-list : {}", interfaceList);
		String mySystemCode = registerProp.getSystemCode();
		if (!mySystemCode.isEmpty()) {
			log.info("Setting property my system-code : {}", mySystemCode);
			AdaptorConstants.MY_SYSTEM_CODE = mySystemCode;
		}
	}
}
