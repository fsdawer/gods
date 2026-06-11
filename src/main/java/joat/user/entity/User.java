package joat.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import joat.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // UUID v7 기반 기본키

    @Column(nullable = false, length = 50)
    private String nickname; // 앱 내 표시 이름 (최대 50자)

    private String profileImageUrl; // S3에 저장된 프로필 이미지 URL (null 허용)

    private String bio; // 자기소개 텍스트 (null 허용)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider oauthProvider; // 소셜 로그인 제공자 (kakao / google)

    @Column(nullable = false)
    private String oauthId; // 소셜 로그인 제공자에서 발급한 고유 ID

    private String fcmToken; // FCM 디바이스 토큰 — 앱 시작 시 저장, null이면 알림 발송 건너뜀

    public static User of(String nickname, OAuthProvider provider, String oauthId) {
        User user = new User();
        user.nickname = nickname;
        user.oauthProvider = provider;
        user.oauthId = oauthId;
        return user;
    }

    public void updateProfile(String nickname, String profileImageUrl, String bio) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
        if (bio != null) this.bio = bio;
    }

    /**
     * FCM 디바이스 토큰을 갱신한다.
     * null 전달 시 토큰을 제거한다 — 로그아웃 시 알림 수신 중단.
     *
     * @param fcmToken 새 FCM 토큰, 또는 null
     */
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
