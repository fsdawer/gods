package joat.global.config;

import joat.auth.jwt.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 프로덕션/개발 서버용 Security 설정 (@Profile("!local")).
 * JwtFilter를 등록하여 Bearer 토큰 인증을 처리한다.
 * 로컬 환경에서는 DevSecurityConfig가 대신 활성화된다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!local")
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /**
     * 프로덕션 Security 필터 체인.
     * - 인증 불필요: /api/auth/**, GET /api/posts/explore, GET /api/tags/**,
     *               GET /api/users/{id}, GET /api/users/{id}/posts,
     *               GET /api/users/{id}/followers, GET /api/users/{id}/following,
     *               /ws/** (WebSocket 핸드셰이크)
     * - 인증 필요: GET /api/users/me (내 프로필), 그 외 모든 요청
     * - JwtFilter가 UsernamePasswordAuthenticationFilter 앞에서 토큰을 검증한다.
     *   인증 실패 시 @AuthenticationPrincipal은 null이 되므로,
     *   비인증 공개 API에서도 viewerId(null)를 통해 isFollowing을 null로 처리할 수 있다.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()                        // 로그인/토큰 갱신 공개
                .requestMatchers(HttpMethod.GET, "/api/posts/explore").permitAll()  // 탐색 피드 공개
                .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()        // 태그 검색/트렌딩 공개
                .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()   // 내 프로필 인증 필요
                .requestMatchers(HttpMethod.GET, "/api/users/*/posts").permitAll()  // 타인 게시물 목록 공개
                .requestMatchers(HttpMethod.GET, "/api/users/*/followers").permitAll()  // 팔로워 목록 공개
                .requestMatchers(HttpMethod.GET, "/api/users/*/following").permitAll()  // 팔로잉 목록 공개
                .requestMatchers(HttpMethod.GET, "/api/users/*").permitAll()        // 타인 프로필 공개
                .requestMatchers("/ws/**", "/ws-native").permitAll()               // WebSocket 핸드셰이크 공개
                .anyRequest().authenticated()                                       // 그 외 인증 필요
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
