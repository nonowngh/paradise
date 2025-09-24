package mb.fw.paradise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import mb.fw.paradise.config.prop.RegisterProp;

@Data
@Configuration
@ConfigurationProperties(prefix = "module", ignoreUnknownFields = true)
public class RegisterModuleConfig {

	private RegisterProp registerProp;
}
