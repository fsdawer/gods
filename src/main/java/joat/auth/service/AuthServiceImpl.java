package joat.auth.service;

import joat.auth.client.GoogleOAuthClient;
import joat.auth.client.KakaoOAuthClient;
import joat.auth.dto.TokenResponse;
import joat.auth.jwt.JwtUtil;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.user.entity.OAuthProvider;
import joat.user.entity.User;
import joat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * AuthService 구현체.
 * 카카오/구글 OAuth 인증 후 JWT를 발급하고 Redis에 refresh token을 관리한다.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** Redis key prefix: auth:refresh:{userId} → refresh token 문자열 */
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";

    private final KakaoOAuthClient kakaoClient;
    private final GoogleOAuthClient googleClient;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    /**
     * [카카오 로그인 플로우]
     * 1. 카카오 API 호출 → 유저 정보(id, nickname) 조회
     * 2. users 테이블에서 oauth_provider='kakao' AND oauth_id=kakaoId 조회
     * 3. 없으면 신규 User 생성 후 저장, 있으면 기존 User 사용
     * 4. JWT 발급 → Redis에 refresh token 저장
     *
     * @param accessToken 카카오 SDK 액세스 토큰 (앱 → 서버로 전달)
     * @return TokenResponse(accessToken, refreshToken)
     */
    @Override
    @Transactional
    public TokenResponse kakaoLogin(String accessToken) {
        // 카카오 API로 유저 정보 조회 (id, nickname 포함)
        var info = kakaoClient.getUserInfo(accessToken);

        // DB에서 kakao oauth_id로 기존 유저 검색, 없으면 신규 가입
        User user = userRepository
            .findByOauthProviderAndOauthId(OAuthProvider.kakao, info.getId())
            .orElseGet(() -> userRepository.save(
                User.of(info.getNickname(), OAuthProvider.kakao, info.getId())));

        // JWT 발급 및 Redis refresh token 저장
        return issueTokens(user.getId());
    }

    /**
     * [구글 로그인 플로우]
     * 1. 구글 API 호출 → 유저 정보(sub, name) 조회
     * 2. users 테이블에서 oauth_provider='google' AND oauth_id=sub 조회
     * 3. 없으면 신규 User 생성 후 저장, 있으면 기존 User 사용
     * 4. JWT 발급 → Redis에 refresh token 저장
     *
     * @param accessToken 구글 SDK 액세스 토큰 (앱 → 서버로 전달)
     * @return TokenResponse(accessToken, refreshToken)
     */
    @Override
    @Transactional
    public TokenResponse googleLogin(String accessToken) {
        // 구글 API로 유저 정보 조회 (sub = 구글 고유 ID)
        var info = googleClient.getUserInfo(accessToken);

        // DB에서 google oauth_id로 기존 유저 검색, 없으면 신규 가입
        User user = userRepository
            .findByOauthProviderAndOauthId(OAuthProvider.google, info.getSub())
            .orElseGet(() -> userRepository.save(
                User.of(info.getName(), OAuthProvider.google, info.getSub())));

        return issueTokens(user.getId());
    }

    /**
     * [Refresh Token 재발급 플로우]
     * 1. refreshToken에서 userId 파싱 (만료/위조 시 BusinessException)
     * 2. Redis에서 "auth:refresh:{userId}" 키로 저장된 토큰과 비교
     * 3. 불일치 시 INVALID_TOKEN 예외
     * 4. 일치하면 새 access/refresh token 쌍 발급
     *
     * @param refreshToken 클라이언트가 보유한 refresh token 문자열
     * @return 새 TokenResponse(accessToken, refreshToken)
     * @throws BusinessException EXPIRED_TOKEN, INVALID_TOKEN
     */
    @Override
    public TokenResponse refresh(String refreshToken) {
        // JWT 파싱으로 userId 추출 (만료 시 EXPIRED_TOKEN 예외)
        UUID userId = jwtUtil.parseUserId(refreshToken);

        // Redis에서 저장된 refresh token과 대조
        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (!refreshToken.equals(stored)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 검증 통과 → 새 토큰 쌍 발급
        return issueTokens(userId);
    }

    /**
     * [로그아웃 플로우]
     * Redis에서 "auth:refresh:{userId}" 키를 삭제하여 refresh token 무효화.
     * 이후 해당 refresh token으로 재발급 요청 시 INVALID_TOKEN 예외 발생.
     *
     * @param userId 로그아웃할 유저의 UUID (JWT에서 파싱된 값)
     */
    @Override
    public void logout(UUID userId) {
        // Redis에서 refresh token 키 삭제 → 해당 유저 세션 만료
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    /**
     * [공통 토큰 발급 내부 메서드]
     * 1. JwtUtil로 access token (30분) + refresh token (14일) 생성
     * 2. Redis에 "auth:refresh:{userId}" = refreshToken 으로 저장 (TTL 14일)
     * 3. TokenResponse 반환
     *
     * @param userId 토큰을 발급할 유저의 UUID
     * @return TokenResponse(accessToken, refreshToken)
     */
    private TokenResponse issueTokens(UUID userId) {
        String access = jwtUtil.generateAccessToken(userId);
        String refresh = jwtUtil.generateRefreshToken(userId);

        // Redis에 refresh token 저장 (TTL: 14일 = 1,209,600초)
        redisTemplate.opsForValue().set(
            REFRESH_KEY_PREFIX + userId,
            refresh,
            Duration.ofSeconds(1209600)
        );

        return new TokenResponse(access, refresh);
    }
}
