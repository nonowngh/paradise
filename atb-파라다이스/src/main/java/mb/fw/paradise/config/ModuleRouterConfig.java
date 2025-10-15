package mb.fw.paradise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.config.annotaion.ConditionalOnAdaptorType;
import mb.fw.paradise.constants.AdaptorConstants;
import mb.fw.paradise.constants.AdaptorType;
import mb.fw.paradise.constants.ESBAPIHeaderConstants;
import mb.fw.paradise.constants.TargetContextPathConstants;
import mb.fw.paradise.module.handler.DBReceiveProcessHandler;
import mb.fw.paradise.module.handler.DBResultProcessHandler;
import mb.fw.paradise.module.handler.ESBAPIServletHandler;
import mb.fw.paradise.module.handler.RFCCallHandler;
import mb.fw.paradise.service.ExceptionService;
import mb.fw.paradise.util.HttpHeaderUtil;

@Slf4j
@Configuration
@ConditionalOnAdaptorType(value = { AdaptorType.INTERFACE_API, AdaptorType.GATEWAY, AdaptorType.RFC_SIMUL }, negate = true)
public class ModuleRouterConfig {

	private final ExceptionService exceptionService;
	private final ModuleConfig config;

	public ModuleRouterConfig(ExceptionService exceptionService, ModuleConfig config) {
		this.exceptionService = exceptionService;
		this.config = config;
	}

	@Bean
	RouterFunction<ServerResponse> receiveRoutes(DBReceiveProcessHandler dbProcessHandler,
			RFCCallHandler rfcCallHandler) {
		return RouterFunctions.route()
				.POST(TargetContextPathConstants.DEFAULT_PATH + AdaptorConstants.MY_SYSTEM_CODE
						+ TargetContextPathConstants.RCV_DB_PROCESS, dbProcessHandler::dbProcess)
				.POST(TargetContextPathConstants.DEFAULT_PATH + AdaptorConstants.MY_SYSTEM_CODE
						+ TargetContextPathConstants.RCV_RFC_CALL, rfcCallHandler::rfcCall)
				.build().filter(logRequestAndResponse()).filter(checkAllowInterfaceList())
				.filter(moduleExceptionHandler());
	}

	@Bean
	RouterFunction<ServerResponse> sendRoutes(DBResultProcessHandler dbResultProcessHandler,
			ESBAPIServletHandler esbAPIServletHandler) {
		return RouterFunctions.route()
				.POST(TargetContextPathConstants.DEFAULT_PATH + AdaptorConstants.MY_SYSTEM_CODE
						+ TargetContextPathConstants.RESULT_DB_PROCESS, dbResultProcessHandler::dbResultProcess)
				.POST(TargetContextPathConstants.DEFAULT_PATH + AdaptorConstants.MY_SYSTEM_CODE
						+ TargetContextPathConstants.SND_COMMON_API, esbAPIServletHandler::callGateway)
				.build().filter(logRequestAndResponse()).filter(checkAllowInterfaceList())
				.filter(moduleResultExceptionHandler());
	}

//	@Bean
//	RouterFunction<ServerResponse> sendAPIRoutes(ESBAPIServletHandler esbAPIServletHandler) {
//		return RouterFunctions.route()
//				.POST(TargetContextPathConstants.DEFAULT_PATH + AdaptorConstants.MY_SYSTEM_CODE
//						+ TargetContextPathConstants.SND_COMMON_API, esbAPIServletHandler::callGateway)
//				.build().filter(logRequestAndResponse()).filter(moduleResultExceptionHandler());
//	}

//	private HandlerFilterFunction<ServerResponse, ServerResponse> moduleExceptionHandler() {
//		return (request, next) -> request.bodyToMono(APIRequestMessage.class).flatMap(dto -> {
//			request.attributes().put("cachedBody", dto);
//			return next.handle(request).onErrorResume(e -> {
//				exceptionService.receiveHandlerExceptionProcess(e, dto);
//				return ServerResponse.noContent().build();
//			});
//		});
//	}

	private HandlerFilterFunction<ServerResponse, ServerResponse> moduleExceptionHandler() {
		return (request, next) -> {
			HttpHeaders headers = request.headers().asHttpHeaders();
			return next.handle(request).onErrorResume(e -> {
				exceptionService.receiveHandlerExceptionProcess(e, headers);
				return ServerResponse.noContent().build();
			});
		};
	}

	private HandlerFilterFunction<ServerResponse, ServerResponse> moduleResultExceptionHandler() {
		return (request, next) -> {
//			HttpHeaders headers = request.headers().asHttpHeaders();
			return next.handle(request).onErrorResume(e -> {
				log.error("Error result handler -> ", e);
				return ServerResponse.noContent().build();
			});
		};
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

	private HandlerFilterFunction<ServerResponse, ServerResponse> checkAllowInterfaceList() {
		return (request, next) -> {
			HttpHeaders headers = request.headers().asHttpHeaders();
			String interfaceId = HttpHeaderUtil.getHeader(headers, ESBAPIHeaderConstants.INTERFACE_ID);
			boolean allowed = config.getInterfaceList().stream().anyMatch(id -> id.equals(interfaceId));
			if (!allowed) {
				exceptionService.receiveHandlerExceptionProcess(new Exception("등록되지 않은 인터페이스 아이디 -> " + interfaceId),
						headers);
				return ServerResponse.noContent().build();
			}
			return next.handle(request);
		};
	}
}
