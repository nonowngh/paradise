package mb.fw.paradise.module.rfc.server;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.rt.DefaultDestinationManager;
import com.sap.conn.jco.server.IndigoJCoServerFactory;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerState;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.common.config.ModuleConfig;
import mb.fw.paradise.common.service.APIService;
import mb.fw.paradise.module.rfc.handler.RfcFunctionHandler;
import mb.fw.paradise.module.rfc.handler.factory.WildcardHandlerFactory;

@Slf4j
@Component
public class RfcServer {
	private JCoServer jcoServer;

	private final JCoDestination destination;
	private final ModuleConfig moduleConfig;
	private final APIService apiService;

	@Autowired
	Environment env;

	public RfcServer(@Qualifier("jcoDestinationServer") JCoDestination destination, ModuleConfig moduleConfig,
			APIService apiService) {
		this.destination = destination;
		this.moduleConfig = moduleConfig;
		this.apiService = apiService;
	}

	@PostConstruct
	public void init() {
		log.info("RFC Server initialized. Scheduling health check...");
		startJcoServer();
	}

	/**
	 * SAP RFC Server Start
	 */
	private synchronized void startJcoServer() {
		try {
			log.info("Starting SAP JCo Server...");

			if (jcoServer != null && JCoServerState.ALIVE.equals(jcoServer.getState())) {
				log.info("JCo jcoServer already started.");
				return;
			}
			connectJCoServer();

			JCoRepository repository = destination.getRepository();
			jcoServer.setRepository(repository);

			// Handler Factory 생성
//			DefaultServerHandlerFactory.FunctionHandlerFactory handlerFactory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
//
//			// RFC별 handler 등록 대신 1개로 등록해서 내부 처리
//			handlerFactory.registerHandler("*",
//					new RfcFunctionHandler(destination.getProperties(), moduleConfig.getInterfaceList(), apiService));

			jcoServer.setCallHandlerFactory(new WildcardHandlerFactory(
					new RfcFunctionHandler(destination.getProperties(), moduleConfig.getInterfaceList(), apiService)));
			jcoServer.addServerErrorListener(new ThrowableListener());
			jcoServer.addServerExceptionListener(new ThrowableListener());

			if (JCoServerState.DEAD.equals(jcoServer.getState())
					|| JCoServerState.STOPPED.equals(jcoServer.getState())) {
				jcoServer.start();
			}

			log.info("SAP JCo RFC Server started.");

		} catch (Exception e) {
			log.error("Failed to start JCo Server -> ", e);
		}
	}

	private void connectJCoServer() throws JCoException {
		DefaultDestinationManager.destinationProp = destination.getProperties();
		jcoServer = IndigoJCoServerFactory.getServerWithProperties("SERVER", destination.getProperties());
	}

	/**
	 * Spring Boot 스케쥴러 기반으로 10초마다 서버 상태 확인 및 재기동
	 */
	@Scheduled(fixedDelayString = "60000")
	public void healthCheck() {
		try {
			if (checkJCoServerHealthy(jcoServer)) {
				log.info("JCo Server is alive.");
			} else {
				log.warn("JCo Server is DEAD. Restarting...");
				startJcoServer();
			}
		} catch (Exception e) {
			log.error("Health check error: ", e);
		}
	}

	/**
	 * JCo Server Health Check
	 */
	private boolean checkJCoServerHealthy(JCoServer server) {
		if (server != null && JCoServerState.ALIVE.equals(server.getState())) {
			try {
				server.getRepository().getFunction("RFC_TRUSTED_CHECK");
				server.getRepository().clear();
				return true;
			} catch (Exception e) {
				log.error("Health Check Error -> ", e);
			}
		}
		return false;
	}

	/**
	 * Error / Exception Listener
	 */
	static class ThrowableListener implements JCoServerErrorListener, JCoServerExceptionListener {
		@Override
		public void serverExceptionOccurred(JCoServer server, String connectionId, JCoServerContextInfo ctx,
				Exception ex) {
			log.error("JCo Server Error -> ", ex);

		}

		@Override
		public void serverErrorOccurred(JCoServer server, String connectionId, JCoServerContextInfo ctx, Error error) {
			log.error("JCo Server Error ->", error);
		}
	}

	@PreDestroy
	public void shutdown() {
		try {
			if (jcoServer != null && !JCoServerState.STOPPED.equals(jcoServer.getState())) {
				log.info("Stopping JCo Server...");
				jcoServer.stop();
			}
		} catch (Exception e) {
			log.error("JCo Server shutdown error -> ", e);
		}
	}

}
