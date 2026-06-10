package joat.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구글 사용자 정보 API 응답 매핑 DTO.
 * GoogleOAuthClient가 https://www.googleapis.com/oauth2/v3/userinfo 응답을 이 클래스로 역직렬화한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfo {
    /** 구글 고유 사용자 식별자 (oauth_id로 저장됨) */
    private String sub;
    /** 구글 계정 표시 이름 (nickname으로 저장됨) */
    private String name;
}
