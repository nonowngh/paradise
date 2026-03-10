package mb.fw.paradise.module.rfc.handler.factory;

import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WildcardHandlerFactory implements JCoServerFunctionHandlerFactory {

	private final JCoServerFunctionHandler universalHandler;

	public WildcardHandlerFactory(JCoServerFunctionHandler handler) {
		this.universalHandler = handler;
	}

	@Override
	public void sessionClosed(JCoServerContext arg0, String arg1, boolean arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public JCoServerFunctionHandler getCallHandler(JCoServerContext serverCtx, String rfcName) {
		log.debug("Incoming RFC : " + rfcName);
		return universalHandler;
	}

}
