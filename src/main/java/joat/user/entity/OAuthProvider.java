package joat.user.entity;

/**
 * 소셜 로그인 제공자 열거형.
 * User 엔티티의 oauthProvider 컬럼에 문자열로 저장된다.
 */
public enum OAuthProvider {
    kakao,  // 카카오 로그인
    google  // 구글 로그인
}
