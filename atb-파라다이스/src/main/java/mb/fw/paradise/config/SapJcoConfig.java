package mb.fw.paradise.config;

import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.ServerDataProvider;

import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.config.prop.SapConnectionProp;
import mb.fw.paradise.constants.AdaptorType;

@Configuration
//@ConditionalOnAdaptorType(value = { AdaptorType.RFC, AdaptorType.RFC_SERVER })
public class SapJcoConfig {

	private final SapConnectionProp properties;

	public SapJcoConfig(SapConnectionProp properties) {
		this.properties = properties;
	}

	@Bean(name = "jcoDestinationClient")
	@ConditionalOnAdaptorType(AdaptorType.RFC)
	JCoDestination jcoDestination() throws Exception {
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
	@ConditionalOnAdaptorType(AdaptorType.RFC_SERVER)
	JCoDestination jcoServerDestination() throws JCoException {
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
