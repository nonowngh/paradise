package mb.fw.paradise.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "sap.connection")
public class SapConnectionProp {
	private String destinationName;
	private String ashost;
	private String sysnr;
	private String client;
	private String user;
	private String passwd;
	private String lang;
	private int poolCapacity;
	private int peakLimit;
}
