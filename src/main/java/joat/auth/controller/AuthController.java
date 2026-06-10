package joat.auth.controller;

import jakarta.validation.Valid;
import joat.auth.dto.OAuthLoginRequest;
import joat.auth.dto.TokenResponse;
import joat.auth.service.AuthService;
import joat.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 인증 관련 API 컨트롤러.
 * 소셜 로그인(카카오/구글), 토큰 재발급, 로그아웃 엔드포인트를 제공한다.
 * SecurityConfig에서 /api/auth/** 는 인증 없이 접근 가능하다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * [엔드포인트] POST /api/auth/oauth/kakao — 카카오 소셜 로그인
     * 앱에서 카카오 SDK로 받은 액세스 토큰을 전달하면 JWT access/refresh token을 반환한다.
     */
    @PostMapping("/oauth/kakao")
    public ApiResponse<TokenResponse> kakaoLogin(@Valid @RequestBody OAuthLoginRequest req) {
        return ApiResponse.ok(authService.kakaoLogin(req.getAccessToken()));
    }

    /**
     * [엔드포인트] POST /api/auth/oauth/google — 구글 소셜 로그인
     * 앱에서 구글 SDK로 받은 액세스 토큰을 전달하면 JWT access/refresh token을 반환한다.
     */
    @PostMapping("/oauth/google")
    public ApiResponse<TokenResponse> googleLogin(@Valid @RequestBody OAuthLoginRequest req) {
        return ApiResponse.ok(authService.googleLogin(req.getAccessToken()));
    }

    /**
     * [엔드포인트] POST /api/auth/token/refresh — 액세스 토큰 재발급
     * refresh token을 전달하면 새 access/refresh token 쌍을 반환한다.
     */
    @PostMapping("/token/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody OAuthLoginRequest req) {
        return ApiResponse.ok(authService.refresh(req.getAccessToken()));
    }

    /**
     * [엔드포인트] DELETE /api/auth/logout — 로그아웃
     * Redis에서 refresh token을 삭제하여 해당 세션을 무효화한다.
     */
    @DeleteMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UUID userId) {
        authService.logout(userId);
        return ApiResponse.ok();
    }
}
