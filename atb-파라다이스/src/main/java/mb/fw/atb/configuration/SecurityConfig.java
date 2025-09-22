package mb.fw.atb.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()  // CSRF 보호 비활성화
                .cors()  // CORS 활성화
                .and()
                .httpBasic()  // HTTP Basic 인증 활성화
                .and()
                .authorizeRequests()
                .antMatchers("/atb-api/**").permitAll()  // 해당 경로는 인증 없이 접근 가능
                .anyRequest().authenticated();  // 다른 모든 요청은 인증 필요
        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");  // 모든 Origin 허용
        config.addAllowedHeader("*");  // 모든 헤더 허용
        config.addAllowedMethod("*");  // 모든 HTTP 메서드 허용
        source.registerCorsConfiguration("/**", config);  // 모든 경로에 CORS 설정 적용
        return new CorsFilter(source);
    }
}