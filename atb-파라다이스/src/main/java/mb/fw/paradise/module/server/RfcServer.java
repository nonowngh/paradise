package mb.fw.paradise.module.server;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.rt.DefaultDestinationManager;
import com.sap.conn.jco.server.DefaultServerHandlerFactory;
import com.sap.conn.jco.server.IndigoJCoServerFactory;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerState;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.config.ModuleConfig;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.module.server.handler.RfcFunctionHandler;
import mb.fw.paradise.service.APIService;

@Slf4j
@Component
@ConditionalOnAdaptorType(AdaptorType.RFC_SERVER)
public class RfcServer {
	public static JCoServer jcoServer;

	private final JCoDestination jcoDestination;
	private final ModuleConfig moduleConfig;
	private final APIService apiService;

	public RfcServer(@Qualifier("jcoDestinationServer") JCoDestination jcoDestination, ModuleConfig moduleConfig,
			APIService apiService) {
		this.jcoDestination = jcoDestination;
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

			JCoRepository repository = jcoDestination.getRepository();
			jcoServer.setRepository(repository);

			// Handler Factory 생성
			DefaultServerHandlerFactory.FunctionHandlerFactory handlerFactory = new DefaultServerHandlerFactory.FunctionHandlerFactory();

			// RFC별 handler 등록 대신 1개로 등록해서 내부 처리
			handlerFactory.registerHandler("*", new RfcFunctionHandler(jcoDestination.getProperties(),
					moduleConfig.getInterfaceList(), apiService));

			jcoServer.setCallHandlerFactory(handlerFactory);
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
		DefaultDestinationManager.destinationProp = jcoDestination.getProperties();
		jcoServer = IndigoJCoServerFactory.getServerWithProperties("SERVER", jcoDestination.getProperties());
	}

	/**
	 * Spring Boot 스케쥴러 기반으로 10초마다 서버 상태 확인 및 재기동
	 */
	@Scheduled(fixedDelayString = "10000")
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
