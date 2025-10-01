package mb.fw.paradise.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("interfaceInfoCache");
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                    .maximumSize(300)
                    .expireAfterWrite(Duration.ofMinutes(60))
        );
        return cacheManager;
    }
}