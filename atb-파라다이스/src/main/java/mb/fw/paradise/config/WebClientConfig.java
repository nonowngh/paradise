package mb.fw.paradise.config;

import javax.net.ssl.SSLException;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.constants.ApiContextPathConstants;
import reactor.netty.http.client.HttpClient;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "web.client", ignoreUnknownFields = true)
public class WebClientConfig {

	@Setter
	String interfaceInfoUrl;

	@Setter
	String gatewayUrl;

	@Bean(name = "interfaceInfoWebClient")
	WebClient interfaceInfoWebClient() {
		return WebClient.builder().baseUrl(interfaceInfoUrl + ApiContextPathConstants.INTERFACE_INFO_API)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
	}

	@Bean(name = "gatewayWebClient")
	WebClient gatewayWebClient() {
		if (gatewayUrl.startsWith("https://")) {
			HttpClient httpClient = HttpClient.create().secure(ssl -> {
				try {
					ssl.sslContext(
							SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
				} catch (SSLException e) {
					log.error("sslContext error!");
				}
			});
			return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
					.baseUrl(gatewayUrl + ApiContextPathConstants.GATEWAY)
					.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
		}
		return WebClient.builder().baseUrl(gatewayUrl + ApiContextPathConstants.GATEWAY)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
	}
}
