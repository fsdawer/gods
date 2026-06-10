package joat.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 로컬 개발 환경 전용 인증 필터 (@Profile("local")).
 * JWT 없이도 API를 테스트할 수 있도록 모든 요청에 고정 UUID(DEV_USER_ID)를
 * SecurityContext principal로 주입한다.
 * users 테이블에 DEV_USER_ID를 가진 유저 행이 있어야 /me 등이 정상 동작한다.
 */
@Component
@Profile("local")
public class DevMockAuthFilter extends OncePerRequestFilter {

    // seed 데이터의 첫 번째 유저 UUID와 일치시켜야 /me 등 정상 동작 (V7__seed_data.sql 참조)
    static final UUID DEV_USER_ID = UUID.fromString("00000001-0000-0000-0000-000000000001");

    /**
     * 매 요청마다 고정 UUID를 SecurityContext에 등록하여 @AuthenticationPrincipal이
     * DEV_USER_ID를 반환하도록 한다. JWT 검증 없이 모든 요청을 인증된 것으로 처리한다.
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(DEV_USER_ID, null, List.of())
        );
        chain.doFilter(request, response);
    }
}
