package mb.fw.paradise.mock.rfc;

import java.util.Properties;

import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import com.sap.conn.jco.rt.AbapFunctionTemplate;

import mb.fw.paradise.mock.rfc.provider.SimpleDestinationDataProvider;
import mb.fw.paradise.mock.rfc.repository.MockRepository;

public class RfcClient {

    public static void main(String[] args) {
        try {
            // 1. 목적지 설정 (mock 서버 설정과 맞춰야 함)
            Properties connectProperties = new Properties();
            connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, "localhost");
            connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, "00");  // mock 서버 sysnr
            connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, "000");
            connectProperties.setProperty(DestinationDataProvider.JCO_USER, "USER");
            connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, "PASS");
            connectProperties.setProperty(DestinationDataProvider.JCO_LANG, "EN");

            // 이름 지정 (아무 이름 가능)
            String DESTINATION_NAME = "MOCK_SERVER";
            
            // 설정 프로바이더 등록
            SimpleDestinationDataProvider provider = new SimpleDestinationDataProvider();
            provider.addDestination(DESTINATION_NAME, connectProperties);
            Environment.registerDestinationDataProvider(provider);


            // 2. Destination 가져오기
            JCoDestination destination = JCoDestinationManager.getDestination(DESTINATION_NAME);

//            // 3. 함수 가져오기 (서버에 등록한 함수명으로)
//            JCoFunction function = destination.getRepository().getFunction("Z_MOCK_FUNCTION");
//            if (function == null) {
//                throw new RuntimeException("Z_MOCK_FUNCTION not found in SAP.");
//            }

         // Repository 수동 주입
            MockRepository repo = new MockRepository("MockRepo");
            // 동일한 함수 정의 필요!
            JCoListMetaData importMeta = JCo.createListMetaData("IMPORT");
            importMeta.add("INPUT", JCoMetaData.TYPE_STRING, 100, 0, 0, null, null, 0, null, null);
            importMeta.lock();

            JCoListMetaData exportMeta = JCo.createListMetaData("EXPORT");
            exportMeta.add("RESULT", JCoMetaData.TYPE_STRING, 100, 0, 0, null, null, 0, null, null);
            exportMeta.lock();

            repo.addFunctionTemplate(new AbapFunctionTemplate("Z_MOCK_FUNCTION", importMeta, exportMeta, null, null, null, false));

            // 직접 함수 호출
            JCoFunction function = repo.getFunctionTemplate("Z_MOCK_FUNCTION").getFunction();
            function.getImportParameterList().setValue("INPUT", "Hello from client!");

            function.execute(destination); // 실제 네트워크 호출은 없지만 형식 맞춰 실행됨

            String result = function.getExportParameterList().getString("RESULT");
            System.out.println("RFC 결과: " + result);

        } catch (JCoException e) {
            e.printStackTrace();
        }
    }
}