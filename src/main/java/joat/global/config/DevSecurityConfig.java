package joat.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 로컬 개발 환경 전용 Security 설정 (@Profile("local")).
 * 프로덕션의 SecurityConfig(JwtFilter) 대신 DevMockAuthFilter를 사용한다.
 * 모든 요청을 허용(permitAll)하고 세션을 STATELESS로 유지한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("local")
public class DevSecurityConfig {

    private final DevMockAuthFilter devMockAuthFilter;

    /**
     * 로컬 환경 Security 필터 체인.
     * CSRF 비활성화, 세션리스, 모든 요청 허용, DevMockAuthFilter 등록.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(devMockAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
