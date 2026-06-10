package joat.auth.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpirySeconds;
    private final long refreshExpirySeconds;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-expiry}") long accessExpirySeconds,
        @Value("${jwt.refresh-expiry}") long refreshExpirySeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirySeconds = accessExpirySeconds;
        this.refreshExpirySeconds = refreshExpirySeconds;
    }

    public String generateAccessToken(UUID userId) {
        return buildToken(userId, accessExpirySeconds);
    }

    public String generateRefreshToken(UUID userId) {
        return buildToken(userId, refreshExpirySeconds);
    }

    public UUID parseUserId(String token) {
        try {
            String sub = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
            return UUID.fromString(sub);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String buildToken(UUID userId, long expirySeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirySeconds * 1000))
            .signWith(key)
            .compact();
    }
}
