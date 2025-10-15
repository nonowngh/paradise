package mb.fw.paradise.module.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import mb.fw.paradise.module.service.RFCModuleService;
import mb.fw.paradise.service.APIService;
import reactor.core.publisher.Mono;

@Component
public class RFCCallHandler {

	@Autowired(required = false)
	RFCModuleService rfcModuleService;

	private final APIService apiService;

	public RFCCallHandler(APIService apiService) {
		this.apiService = apiService;
	}

	public Mono<ServerResponse> rfcCall(ServerRequest serverRequest) {
		return null;

	}
}
