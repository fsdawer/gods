package joat.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 토큰 등록/갱신 요청 DTO.
 * token이 null이면 토큰 제거 (로그아웃 시 사용).
 */
@Getter
@NoArgsConstructor
public class FcmTokenRequest {

    /** FCM 디바이스 토큰. null 전달 시 기존 토큰을 제거한다. */
    private String token;
}
