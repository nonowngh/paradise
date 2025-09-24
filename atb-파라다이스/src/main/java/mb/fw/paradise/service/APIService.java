package mb.fw.paradise.service;

import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.constants.TargetModule;
import mb.fw.paradise.dto.APIReqeustMessage;
import mb.fw.paradise.dto.APIResponseMessage;
import reactor.core.publisher.Mono;

@Service
public class APIService {

	private final WebClient interfaceInfoWebClient;
	private final WebClient gatewayWebClient;

	public APIService(@Qualifier("interfaceInfoWebClient") WebClient interfaceInfoWebClient,
			@Qualifier("gatewayWebClient") WebClient gatewayWebClient) {
		this.interfaceInfoWebClient = interfaceInfoWebClient;
		this.gatewayWebClient = gatewayWebClient;
	}

	public InterfaceInfo getInterfaceInfo(String interfaceId) {
		return interfaceInfoWebClient.get().uri(uriBuilder -> uriBuilder.queryParam("interfaceId", interfaceId).build())
				.retrieve().bodyToMono(InterfaceInfo.class)
				.switchIfEmpty(Mono.error(new NoSuchElementException("InterfaceInfo not found for id : " + interfaceId)))
				.block();
	}

	public Mono<APIResponseMessage> callGateway(APIReqeustMessage request, TargetModule targetModule) {
		return gatewayWebClient.post().uri(targetModule.getContextPath()).bodyValue(request).retrieve()
				.onStatus(HttpStatus::isError,
						clientResponse -> clientResponse.bodyToMono(String.class)
								.flatMap(errorBody -> Mono.error(new RuntimeException("Error: " + errorBody))))
				.bodyToMono(APIResponseMessage.class);
	}
}
