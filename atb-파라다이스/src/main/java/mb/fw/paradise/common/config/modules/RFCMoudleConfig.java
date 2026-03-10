package mb.fw.paradise.common.config.modules;

import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.ServerDataProvider;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.common.constants.ModuleConfigConstants;

@Slf4j
@Data
@Configuration
@ComponentScan(basePackages = ModuleConfigConstants.RFC_PACKAGE)
@EnableConfigurationProperties(RFCMoudleConfig.SapConnectionProp.class) // 내부 프로퍼티 클래스 활성화
@ConfigurationProperties(prefix = ModuleConfigConstants.RFC_PREFIX, ignoreUnknownFields = true)
@ConditionalOnProperty(prefix = ModuleConfigConstants.RFC_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = false)
public class RFCMoudleConfig {

	/**
     * 내부 정적 클래스로 프로퍼티 정의
     */
    @Data
    @ConfigurationProperties(prefix = ModuleConfigConstants.RFC_PREFIX + ".connection")
    public static class SapConnectionProp {
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
        
        // Server setting
        private String gwhost;
        private String gwserv;
        private int maxStartupDelay;
        private String progid;
        private String connectionCount;
    }

	@Bean(name = "jcoDestinationClient")
	JCoDestination jcoDestination(SapConnectionProp properties) throws Exception {
		Properties connectProperties = new Properties();
		connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, properties.getAshost());
		connectProperties.setProperty(DestinationDataProvider.JCO_R3NAME, properties.getR3name());
		connectProperties.setProperty(DestinationDataProvider.JCO_MSSERV, properties.getMsserv());
		connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, properties.getClient());
		connectProperties.setProperty(DestinationDataProvider.JCO_USER, properties.getUser());
		connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, properties.getPasswd());
		connectProperties.setProperty(DestinationDataProvider.JCO_LANG, properties.getLang());
		connectProperties.setProperty(DestinationDataProvider.JCO_GROUP, properties.getGroup());
		connectProperties.setProperty(DestinationDataProvider.JCO_TRACE, properties.getTrace());
		connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, properties.getSysnr());
//		connectProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY,
//				String.valueOf(properties.getPoolCapacity()));
//		connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT,
//				String.valueOf(properties.getPeakLimit()));
		return JCoDestinationManager.getDestinationForIndigo(properties.getDestinationName(), connectProperties);
	}
	
	@Bean(name = "jcoDestinationServer")
	JCoDestination jcoServerDestination(SapConnectionProp properties) throws JCoException {
		Properties connectProperties = new Properties();
		connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, properties.getAshost());
//		connectProperties.setProperty(DestinationDataProvider.JCO_R3NAME, properties.getR3name());
//		connectProperties.setProperty(DestinationDataProvider.JCO_MSSERV, properties.getMsserv());
		connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, properties.getClient());
		connectProperties.setProperty(DestinationDataProvider.JCO_USER, properties.getUser());
		connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, properties.getPasswd());
		connectProperties.setProperty(DestinationDataProvider.JCO_LANG, properties.getLang());
//		connectProperties.setProperty(DestinationDataProvider.JCO_GROUP, properties.getGroup());
		connectProperties.setProperty(DestinationDataProvider.JCO_TRACE, properties.getTrace());
		connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, properties.getSysnr());
		connectProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY,
				String.valueOf(properties.getPoolCapacity()));
		connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT,
				String.valueOf(properties.getPeakLimit()));

		connectProperties.setProperty(ServerDataProvider.JCO_GWHOST, properties.getGwhost());
		connectProperties.setProperty(ServerDataProvider.JCO_GWSERV, properties.getGwserv());
		connectProperties.setProperty(ServerDataProvider.JCO_PROGID, properties.getProgid());
		connectProperties.setProperty(ServerDataProvider.JCO_MAX_STARTUP_DELAY,
				String.valueOf(properties.getMaxStartupDelay()));
		connectProperties.setProperty(ServerDataProvider.JCO_CONNECTION_COUNT,
				String.valueOf(properties.getConnectionCount()));

		return JCoDestinationManager.getDestinationForIndigo(properties.getDestinationName(), connectProperties);
	}
}
