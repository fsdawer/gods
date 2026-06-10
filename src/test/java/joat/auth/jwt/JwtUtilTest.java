package joat.auth.jwt;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, 1800L, 1209600L);
    }

    @Test
    void 액세스_토큰을_생성하고_유저ID를_파싱할_수_있다() {
        String token = jwtUtil.generateAccessToken(userId);
        UUID parsed = jwtUtil.parseUserId(token);
        assertThat(parsed).isEqualTo(userId);
    }

    @Test
    void 만료된_토큰은_EXPIRED_TOKEN_예외를_던진다() {
        JwtUtil shortLived = new JwtUtil(secret, 0L, 0L);
        String token = shortLived.generateAccessToken(userId);
        assertThatThrownBy(() -> shortLived.parseUserId(token))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void 잘못된_토큰은_INVALID_TOKEN_예외를_던진다() {
        assertThatThrownBy(() -> jwtUtil.parseUserId("not.a.jwt"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_TOKEN);
    }
}
