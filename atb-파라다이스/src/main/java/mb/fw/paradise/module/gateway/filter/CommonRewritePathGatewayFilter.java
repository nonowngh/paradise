package mb.fw.paradise.module.gateway.filter;

import java.net.URI;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

//@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommonRewritePathGatewayFilter implements GlobalFilter {

    private final RouteLocator routeLocator;
    public CommonRewritePathGatewayFilter(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
//        RewritePathGatewayFilterFactory.Config config = new RewritePathGatewayFilterFactory.Config()
//                .setRegexp("/esb/api/gateway/(?<remaining>.*)")
//                .setReplacement("/esb/api/${remaining}");
//
//        return rewriteFactory.apply(config).filter(exchange, chain);

    	ServerHttpRequest originalRequest = exchange.getRequest();

        // path rewrite
        String rewrittenPath = originalRequest.getURI().getPath()
                .replaceFirst("/esb/api/gateway/", "/esb/api/");

        // route ID 추출 (여기서는 Path Predicate 기준)
        String routeId = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR);

        if (routeId == null) {
            return chain.filter(exchange);
        }

        return routeLocator.getRoutes()
                .filter(route -> route.getId().equals(routeId))
                .next()
                .flatMap(route -> {
                    URI routeUri = route.getUri(); // YAML에 설정된 URI 가져오기

                    // 새로운 URI 생성 (host/port는 routeUri 사용, path는 rewrite)
                    URI newUri = URI.create(routeUri.toString() + rewrittenPath +
                            (originalRequest.getURI().getQuery() != null ? "?" + originalRequest.getURI().getQuery() : ""));

                    ServerHttpRequest newRequest = originalRequest.mutate()
                            .path(rewrittenPath)
                            .build();

                    ServerWebExchange newExchange = exchange.mutate()
                            .request(newRequest)
                            .build();

                    newExchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, newUri);

                    return chain.filter(newExchange);
                });
    }

}