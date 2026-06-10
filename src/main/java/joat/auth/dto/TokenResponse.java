package joat.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JWT 토큰 발급 응답 DTO.
 * 로그인 및 토큰 재발급 시 access token과 refresh token을 함께 반환한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    /** JWT 액세스 토큰 (유효기간 30분, API 요청 헤더에 사용) */
    private String accessToken;
    /** JWT 리프레시 토큰 (유효기간 14일, 액세스 토큰 재발급 시 사용) */
    private String refreshToken;
}
