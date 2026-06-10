package joat.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth 소셜 로그인 요청 DTO.
 * 카카오/구글 SDK에서 발급받은 액세스 토큰을 서버로 전달할 때 사용한다.
 * 토큰 재발급(/token/refresh) 엔드포인트에서도 refresh token 전달에 재사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginRequest {
    @NotBlank
    /** 카카오/구글 SDK 액세스 토큰 또는 JWT refresh token */
    private String accessToken;
}
