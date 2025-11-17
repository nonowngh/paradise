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
	private String r3name;
	private String msserv;
	private String client;
	private String user;
	private String passwd;
	private String lang;
	private String group;
	private String trace;
	private String sysnr;
	private int poolCapacity = 3;
	private int peakLimit = 10;
	//server setting..
	private String gwhost;
	private String gwserv;
	private int maxStartupDelay;
	private String progid;
	private String connectionCount;
}
