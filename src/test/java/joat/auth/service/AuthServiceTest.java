package joat.auth.service;

import joat.auth.client.KakaoOAuthClient;
import joat.auth.dto.KakaoUserInfo;
import joat.auth.dto.TokenResponse;
import joat.auth.jwt.JwtUtil;
import joat.user.entity.OAuthProvider;
import joat.user.entity.User;
import joat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks AuthServiceImpl authService;
    @Mock KakaoOAuthClient kakaoOAuthClient;
    @Mock UserRepository userRepository;
    @Mock JwtUtil jwtUtil;
    @Mock StringRedisTemplate redisTemplate;

    @Test
    void 카카오_로그인_신규유저는_저장_후_토큰을_반환한다() {
        var userInfo = new KakaoUserInfo("kakao-123",
            new KakaoUserInfo.KakaoAccount(new KakaoUserInfo.Profile("갓생러")));
        given(kakaoOAuthClient.getUserInfo("token")).willReturn(userInfo);
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.kakao, "kakao-123"))
            .willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(jwtUtil.generateAccessToken(any())).willReturn("access-token");
        given(jwtUtil.generateRefreshToken(any())).willReturn("refresh-token");
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        TokenResponse result = authService.kakaoLogin("token");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void 카카오_로그인_기존유저는_저장_없이_토큰을_반환한다() {
        var userInfo = new KakaoUserInfo("kakao-123",
            new KakaoUserInfo.KakaoAccount(new KakaoUserInfo.Profile("갓생러")));
        User existing = User.of("갓생러", OAuthProvider.kakao, "kakao-123");
        given(kakaoOAuthClient.getUserInfo("token")).willReturn(userInfo);
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.kakao, "kakao-123"))
            .willReturn(Optional.of(existing));
        given(jwtUtil.generateAccessToken(any())).willReturn("access-token");
        given(jwtUtil.generateRefreshToken(any())).willReturn("refresh-token");
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        TokenResponse result = authService.kakaoLogin("token");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        then(userRepository).should().findByOauthProviderAndOauthId(OAuthProvider.kakao, "kakao-123");
    }
}
