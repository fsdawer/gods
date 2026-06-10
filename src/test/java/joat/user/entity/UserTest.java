package joat.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void of_팩토리로_유저를_생성할_수_있다() {
        User user = User.of("갓생러", OAuthProvider.kakao, "kakao-123");

        assertThat(user.getNickname()).isEqualTo("갓생러");
        assertThat(user.getOauthProvider()).isEqualTo(OAuthProvider.kakao);
        assertThat(user.getOauthId()).isEqualTo("kakao-123");
    }

    @Test
    void updateProfile_null이면_기존값_유지() {
        User user = User.of("갓생러", OAuthProvider.kakao, "kakao-123");
        user.updateProfile(null, null, "열심히 살기");

        assertThat(user.getNickname()).isEqualTo("갓생러");
        assertThat(user.getBio()).isEqualTo("열심히 살기");
    }
}
