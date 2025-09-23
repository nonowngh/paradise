package mb.fw.paradise.filter;

import java.util.Optional;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.service.LoggingService;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(1)
public class GatewayLoggingFilter implements GlobalFilter {

	private final Optional<JmsTemplate> jmsTemplate;
	private LoggingService loggingService;

	public GatewayLoggingFilter(Optional<JmsTemplate> jmsTemplate, LoggingService loggingService) {
		this.jmsTemplate = jmsTemplate;
		this.loggingService = loggingService;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// 요청 로깅
		log.info("[gateway-logging-filter] Request: " + exchange.getRequest().getMethod() + " "
				+ exchange.getRequest().getURI());

		jmsTemplate.ifPresent(jms -> {
			loggingService.asyncStartLogging(jms, null, null, null, null);
		});

		return chain.filter(exchange).then(Mono.fromRunnable(() -> {
			try {
				// 응답 로깅
				log.info("[gateway-logging-filter] Response Status Code: " + exchange.getResponse().getStatusCode());

				jmsTemplate.ifPresent(jms -> {
					loggingService.asyncEndLogging(jms, null, null, 0, null, null);
				});
			} catch (Exception e) {
				  log.error("Unexpected error in GlobalLoggingFilter", e);
			}
		}));
	}

}
