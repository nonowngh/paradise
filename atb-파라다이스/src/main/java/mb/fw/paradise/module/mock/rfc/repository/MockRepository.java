package mb.fw.paradise.module.mock.rfc.repository;

import java.util.HashMap;
import java.util.Map;

import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.rt.CustomRepository;

public class MockRepository extends CustomRepository {

    private final Map<String, JCoFunctionTemplate> functions = new HashMap<>();

    public MockRepository(String name) {
        super(name);
    }

    public void addFunctionTemplate(JCoFunctionTemplate template) {
        functions.put(template.getName(), template);
    }

    @Override
    public JCoFunctionTemplate getFunctionTemplate(String functionName) {
        return functions.get(functionName);
    }
}
