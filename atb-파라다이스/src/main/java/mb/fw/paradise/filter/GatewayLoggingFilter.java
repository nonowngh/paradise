package mb.fw.paradise.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(1)
public class GatewayLoggingFilter implements GlobalFilter {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// 요청 로깅
		log.info("[GlobalLoggingFilter] Request: " + exchange.getRequest().getMethod() + " "
				+ exchange.getRequest().getURI());

		return chain.filter(exchange).then(Mono.fromRunnable(() -> {
			// 응답 로깅
			log.info("[GlobalLoggingFilter] Response Status Code: " + exchange.getResponse().getStatusCode());
		}));
	}

}
