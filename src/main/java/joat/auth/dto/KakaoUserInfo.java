package joat.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 사용자 정보 API 응답 매핑 DTO.
 * KakaoOAuthClient가 https://kapi.kakao.com/v2/user/me 응답을 이 클래스로 역직렬화한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoUserInfo {

    /** 카카오 고유 사용자 ID (oauth_id로 저장됨) */
    private String id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount; // 카카오 계정 정보 (프로필 포함)

    /**
     * 닉네임 추출 헬퍼.
     * kakao_account.profile.nickname이 없으면 기본값 "갓생러"를 반환한다.
     */
    public String getNickname() {
        if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
            return kakaoAccount.getProfile().getNickname();
        }
        return "갓생러";
    }

    /** 카카오 계정 중첩 객체 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoAccount {
        private Profile profile; // 프로필 정보 (닉네임 포함)
    }

    /** 카카오 프로필 중첩 객체 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {
        private String nickname; // 카카오 닉네임
    }
}
