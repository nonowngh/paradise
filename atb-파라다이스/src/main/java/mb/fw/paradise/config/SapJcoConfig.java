package mb.fw.paradise.config;

import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.ext.DestinationDataProvider;

import mb.fw.paradise.config.prop.SapConnectionProp;

@Configuration
public class SapJcoConfig {

	private final SapConnectionProp properties;

	public SapJcoConfig(SapConnectionProp properties) {
		this.properties = properties;
	}

	@Bean
	JCoDestination jcoDestination() throws Exception {
		Properties connectProperties = new Properties();
		connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, properties.getAshost());
		connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, properties.getSysnr());
		connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, properties.getClient());
		connectProperties.setProperty(DestinationDataProvider.JCO_USER, properties.getUser());
		connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, properties.getPasswd());
		connectProperties.setProperty(DestinationDataProvider.JCO_LANG, properties.getLang());
		connectProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY,
				String.valueOf(properties.getPoolCapacity()));
		connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT,
				String.valueOf(properties.getPeakLimit()));

		return JCoDestinationManager.getDestination(properties.getDestinationName());
	}
}
