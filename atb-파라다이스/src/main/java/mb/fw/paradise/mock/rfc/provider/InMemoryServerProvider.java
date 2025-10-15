package mb.fw.paradise.mock.rfc.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.sap.conn.jco.ext.ServerDataEventListener;
import com.sap.conn.jco.ext.ServerDataProvider;

public class InMemoryServerProvider implements ServerDataProvider {

	private final Map<String, Properties> serverConfigs = new HashMap<>();

	public void addServerProperties(String serverName, Properties props) {
		serverConfigs.put(serverName, props);
	}

	@Override
	public Properties getServerProperties(String serverName) {
		return serverConfigs.get(serverName);
	}

	@Override
	public boolean supportsEvents() {
		return false;
	}

	@Override
	public void setServerDataEventListener(ServerDataEventListener listener) {
		// Optional
	}

}
