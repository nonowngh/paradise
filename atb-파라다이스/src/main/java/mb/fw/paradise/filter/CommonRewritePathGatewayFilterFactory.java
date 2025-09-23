package mb.fw.paradise.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class CommonRewritePathGatewayFilterFactory extends AbstractGatewayFilterFactory<CommonRewritePathGatewayFilterFactory.Config>  {

	public CommonRewritePathGatewayFilterFactory() {
        super(Config.class);
    }
	
	@Override
	public GatewayFilter apply(Config  config) {
		RewritePathGatewayFilterFactory rewriteFactory = new RewritePathGatewayFilterFactory();

        RewritePathGatewayFilterFactory.Config innerConfig = new RewritePathGatewayFilterFactory.Config()
            .setRegexp("/esb/api/gateway/(?<remaining>.*)")
            .setReplacement("/esb/api/${remaining}");

        return rewriteFactory.apply(innerConfig);
	}

    public static class Config {
    }
}
