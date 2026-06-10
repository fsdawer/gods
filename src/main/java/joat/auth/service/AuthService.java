package joat.auth.service;

import joat.auth.dto.TokenResponse;

import java.util.UUID;

/**
 * 인증 서비스 인터페이스.
 * OAuth 소셜 로그인, JWT 토큰 발급/갱신, 로그아웃을 담당한다.
 */
public interface AuthService {

    /**
     * 카카오 OAuth 로그인.
     *
     * @param accessToken 카카오 SDK에서 발급받은 액세스 토큰
     * @return JWT access token + refresh token 쌍
     */
    TokenResponse kakaoLogin(String accessToken);

    /**
     * 구글 OAuth 로그인.
     *
     * @param accessToken 구글 SDK에서 발급받은 액세스 토큰
     * @return JWT access token + refresh token 쌍
     */
    TokenResponse googleLogin(String accessToken);

    /**
     * Refresh Token으로 새 Access Token 발급.
     *
     * @param refreshToken Redis에 저장된 refresh token 문자열
     * @return 새로운 JWT access token + refresh token 쌍
     */
    TokenResponse refresh(String refreshToken);

    /**
     * 로그아웃 — Redis에서 refresh token 삭제.
     *
     * @param userId 로그아웃할 유저의 UUID
     */
    void logout(UUID userId);
}
