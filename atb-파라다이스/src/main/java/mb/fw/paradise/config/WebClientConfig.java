package mb.fw.paradise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import mb.fw.paradise.constants.APIContextPathConstants;

@Configuration
public class WebClientConfig {

	@Bean(name = "interfaceInfoWebClient")
	WebClient interfaceInfoWebClient() {
		return WebClient.builder().baseUrl(APIContextPathConstants.INTERFACE_INFO_API)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
	}

	@Bean(name = "gatewayWebClient")
	WebClient gatewayWebClient() {
		return WebClient.builder().baseUrl(APIContextPathConstants.GATEWAY)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
	}
}
