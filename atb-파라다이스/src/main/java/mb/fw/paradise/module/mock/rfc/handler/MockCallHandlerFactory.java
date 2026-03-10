package mb.fw.paradise.module.mock.rfc.handler;

import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;

public class MockCallHandlerFactory implements JCoServerFunctionHandlerFactory {
	@Override
	public JCoServerFunctionHandler getCallHandler(JCoServerContext serverCtx, String functionName) {
		if ("Z_MOCK_FUNCTION".equals(functionName)) {
			return new FunctionHandler();
		}
		return null;
	}

	@Override
	public void sessionClosed(JCoServerContext arg0, String arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
	}
}
