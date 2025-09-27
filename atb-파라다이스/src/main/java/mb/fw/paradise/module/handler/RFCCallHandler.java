package mb.fw.paradise.module.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@Component
public class RFCCallHandler {

	public Mono<ServerResponse> rfcCall(ServerRequest serverRequest) {
		return null;
		
	}
}
