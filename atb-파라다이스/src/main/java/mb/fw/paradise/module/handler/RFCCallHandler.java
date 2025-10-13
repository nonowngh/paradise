package mb.fw.paradise.module.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import mb.fw.paradise.module.service.RFCModuleService;
import mb.fw.paradise.service.APIService;
import reactor.core.publisher.Mono;

@Component
public class RFCCallHandler {
	
	private final RFCModuleService rfcModuleService;
	private final APIService apiService;
	
	public RFCCallHandler(APIService apiService, RFCModuleService rfcModuleService) {
		this.rfcModuleService = rfcModuleService;
		this.apiService = apiService;
	}

	public Mono<ServerResponse> rfcCall(ServerRequest serverRequest) {
		return null;
		
	}
}
