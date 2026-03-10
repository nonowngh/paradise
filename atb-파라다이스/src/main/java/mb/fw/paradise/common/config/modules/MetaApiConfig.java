package mb.fw.paradise.common.config.modules;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.Data;
import mb.fw.paradise.common.constants.ModuleConfigConstants;

@Data
@EnableCaching
@Configuration
@ComponentScan(basePackages = ModuleConfigConstants.METAAPI_PACKAGE)
@ConfigurationProperties(prefix = ModuleConfigConstants.METAAPI_PREFIX, ignoreUnknownFields = true)
@ConditionalOnProperty(prefix = ModuleConfigConstants.METAAPI_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = false)
public class MetaApiConfig {
	
    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("MetaInfoCache");
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                    .maximumSize(300)
                    .expireAfterWrite(Duration.ofMinutes(60))
        );
        return cacheManager;
    }
}
