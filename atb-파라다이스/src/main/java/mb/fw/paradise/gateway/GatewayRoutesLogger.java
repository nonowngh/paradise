package mb.fw.paradise.gateway;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GatewayRoutesLogger {
	private final RouteLocator routeLocator;

	public GatewayRoutesLogger(RouteLocator routeLocator) {
		this.routeLocator = routeLocator;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logRoutes() {
		routeLocator.getRoutes().subscribe(route -> {
			log.info("🛣️ Route ID      : " + route.getId());
			log.info("➡️ URI           : " + route.getUri());
			log.info("📌 Predicates    : " + route.getPredicate());
			log.info("🔧 Filters       : " + route.getFilters());
			log.info("----------------------------------------");
		});
	}
}
