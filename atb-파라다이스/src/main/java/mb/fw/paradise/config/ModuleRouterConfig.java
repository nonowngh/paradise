package mb.fw.paradise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.AdaptorConstants;
import mb.fw.paradise.constants.TargetContextPathConstants;
import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import mb.fw.paradise.module.handler.DBReceiveProcessHandler;
import mb.fw.paradise.module.handler.DBResultProcessHandler;
import mb.fw.paradise.module.handler.RFCCallHandler;
import mb.fw.paradise.service.ExceptionService;

@Slf4j
@Configuration
public class ModuleRouterConfig {

	private final ExceptionService exceptionService;

	public ModuleRouterConfig(ExceptionService exceptionService) {
		this.exceptionService = exceptionService;
	}

	@Bean
	RouterFunction<ServerResponse> receiveRoutes(DBReceiveProcessHandler dbProcessHandler, RFCCallHandler rfcCallHandler) {
		return RouterFunctions.route()
				.POST(TargetContextPathConstants.DEFAULT_PATH + AdaptorConstants.MY_SYSTEM_CODE
						+ TargetContextPathConstants.RCV_DB_PROCESS, dbProcessHandler::dbProcess)
				.POST(TargetContextPathConstants.DEFAULT_PATH + AdaptorConstants.MY_SYSTEM_CODE
						+ TargetContextPathConstants.RCV_RFC_CALL, rfcCallHandler::rfcCall)
				.build().filter(logRequestAndResponse()).filter(moduleExceptionHandler());
	}
	
	@Bean
	RouterFunction<ServerResponse> sendRoutes(DBResultProcessHandler DBResultProcessHandler) {
		return RouterFunctions.route()
				.POST(TargetContextPathConstants.DEFAULT_PATH + AdaptorConstants.MY_SYSTEM_CODE
						+ TargetContextPathConstants.RESULT_DB_PROCESS, DBResultProcessHandler::dbResultProcess)
				.build().filter(logRequestAndResponse()).filter(moduleResultExceptionHandler());
	}

	private HandlerFilterFunction<ServerResponse, ServerResponse> moduleExceptionHandler() {
		return (request, next) -> request.bodyToMono(APIRequestMessage.class).flatMap(dto -> {
			request.attributes().put("cachedBody", dto);
			return next.handle(request).onErrorResume(e -> {
				exceptionService.exceptionProcess(e, dto);
				return ServerResponse.noContent().build();
			});
		});
	}
	
	private HandlerFilterFunction<ServerResponse, ServerResponse> moduleResultExceptionHandler() {
		return (request, next) -> request.bodyToMono(APIResponseMessage.class).flatMap(dto -> {
			request.attributes().put("cachedBody", dto);
			return next.handle(request).onErrorResume(e -> {
				log.error("Error result handler -> ", e);
				return ServerResponse.noContent().build();
			});
		});
	}

	private HandlerFilterFunction<ServerResponse, ServerResponse> logRequestAndResponse() {
		return (request, next) -> {
			long startTime = System.currentTimeMillis();
			// 요청 정보 로그
			log.info("[REQ] {} {}", request.methodName(), request.path());
			return next.handle(request).doOnSuccess(response -> {
				long duration = System.currentTimeMillis() - startTime;
				log.info("[RES] {} {} ({} ms)", request.methodName(), request.path(), duration);
			}).doOnError(e -> {
				long duration = System.currentTimeMillis() - startTime;
				log.error("[ERROR] {} {} ({} ms) - {}", request.methodName(), request.path(), duration, e.getMessage());
			});
		};
	}
}
