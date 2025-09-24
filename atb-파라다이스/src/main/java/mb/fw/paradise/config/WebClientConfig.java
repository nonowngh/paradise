package mb.fw.paradise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.Setter;
import mb.fw.paradise.constants.APIContextPathConstants;

@Configuration
@ConfigurationProperties(prefix = "web.client", ignoreUnknownFields = true)
public class WebClientConfig {
	
	@Setter
	String interfaceInfoUrl;
	
	@Setter
	String gatewayUrl;

	@Bean(name = "interfaceInfoWebClient")
	WebClient interfaceInfoWebClient() {
		return WebClient.builder().baseUrl(interfaceInfoUrl + APIContextPathConstants.INTERFACE_INFO_API)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
	}

	@Bean(name = "gatewayWebClient")
	WebClient gatewayWebClient() {
		return WebClient.builder().baseUrl(gatewayUrl + APIContextPathConstants.GATEWAY)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
	}
}
