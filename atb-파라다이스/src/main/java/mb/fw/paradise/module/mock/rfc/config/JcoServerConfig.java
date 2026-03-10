package mb.fw.paradise.module.mock.rfc.config;

import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.ext.Environment;
import com.sap.conn.jco.ext.ServerDataProvider;
import com.sap.conn.jco.rt.AbapFunctionTemplate;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerFactory;

import mb.fw.paradise.module.mock.rfc.handler.MockCallHandlerFactory;
import mb.fw.paradise.module.mock.rfc.provider.InMemoryServerProvider;
import mb.fw.paradise.module.mock.rfc.repository.MockRepository;

@Configuration
public class JcoServerConfig {

	@PostConstruct
	public void startServer() throws JCoException {
		String serverName = "MY_SERVER";

		// 1. 메모리 기반 설정 등록
		Properties props = new Properties();
		props.setProperty(ServerDataProvider.JCO_GWHOST, "localhost");
		props.setProperty(ServerDataProvider.JCO_GWSERV, "3300");
		props.setProperty(ServerDataProvider.JCO_PROGID, "MOCK_SERVER");
		props.setProperty(ServerDataProvider.JCO_CONNECTION_COUNT, "2");
//        props.setProperty(ServerDataProvider.JCO_REP_DEST, "SAP_DEV");

		// 2. Provider 등록
		InMemoryServerProvider provider = new InMemoryServerProvider();
		provider.addServerProperties(serverName, props);

		if (!Environment.isServerDataProviderRegistered()) {
			Environment.registerServerDataProvider(provider);
		}

		// 3. 서버 객체 생성
		JCoServer server = JCoServerFactory.getServer(serverName);

		
		server.setRepository(createCustomRepository());

		// 4. 함수 핸들러 설정
		server.setCallHandlerFactory(new MockCallHandlerFactory());

		// 5. 서버 시작
		server.start();

		System.out.println("SAP RFC Mock Server started: " + serverName);
	}

	public MockRepository createCustomRepository() {
		// 리포지토리 이름 지정 (아무 이름 가능)
		MockRepository repository = new MockRepository("MockRepo");

		// 함수 이름 지정
		String functionName = "Z_MOCK_FUNCTION";

		// Import 파라미터 메타데이터 생성
		JCoListMetaData importMeta = JCo.createListMetaData("IMPORT");
		importMeta.add("INPUT", JCoMetaData.TYPE_STRING, 50, 0, 0, null, null, 0, null, null);
		importMeta.lock();

		// Export 파라미터 메타데이터 생성
		JCoListMetaData exportMeta = JCo.createListMetaData("EXPORT");
		exportMeta.add("RESULT", JCoMetaData.TYPE_STRING, 50, 0, 0, null, null, 0, null, null);
		exportMeta.lock();

		// 함수 템플릿 생성 (Import, Export, Tables)
		AbapFunctionTemplate functionTemplate = new AbapFunctionTemplate(functionName, importMeta, exportMeta, null,
				null, null, false);

		// 함수 템플릿 리포지토리에 추가
		repository.addFunctionTemplate(functionTemplate);

		return repository;
	}
}