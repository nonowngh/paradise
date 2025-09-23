package mb.fw.paradise.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import com.indigo.indigomq.pool.PooledConnectionFactory;

@Configuration
@ConditionalOnProperty(name = "jms.logging.enabled", havingValue = "true")
public class JmsTemplateConfig {

	@Bean
	JmsTemplate jmsTemplate(PooledConnectionFactory jmsConnectionFactory) {
		return new JmsTemplate(jmsConnectionFactory);
	}
}
