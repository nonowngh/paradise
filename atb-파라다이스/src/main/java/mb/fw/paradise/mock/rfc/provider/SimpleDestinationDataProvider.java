package mb.fw.paradise.mock.rfc.provider;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;

public class SimpleDestinationDataProvider implements DestinationDataProvider {

	private final Map<String, Properties> destinations = new ConcurrentHashMap<>();

	public void addDestination(String name, Properties properties) {
		destinations.put(name, properties);
	}

	@Override
	public Properties getDestinationProperties(String destinationName) {
		return destinations.get(destinationName);
	}

	@Override
	public void setDestinationDataEventListener(DestinationDataEventListener listener) {
		// Not implemented for simplicity
	}

	@Override
	public boolean supportsEvents() {
		return false;
	}

}
