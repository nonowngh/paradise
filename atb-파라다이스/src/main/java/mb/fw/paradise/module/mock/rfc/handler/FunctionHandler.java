package mb.fw.paradise.module.mock.rfc.handler;

import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;

public class FunctionHandler implements JCoServerFunctionHandler {
	@Override
	public void handleRequest(JCoServerContext ctx, JCoFunction function) throws AbapException {
		String input = function.getImportParameterList().getString("INPUT");
		function.getExportParameterList().setValue("RESULT", "응답: " + input);
	}
}