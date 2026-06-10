package joat.user.repository;

import joat.user.entity.OAuthProvider;
import joat.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    // 소셜 로그인 제공자와 해당 제공자의 고유 ID로 기존 유저를 조회 (신규 가입 여부 판단)
    Optional<User> findByOauthProviderAndOauthId(OAuthProvider provider, String oauthId);

    // 닉네임 부분 일치 검색 (대소문자 무시)
    List<User> findByNicknameContainingIgnoreCase(String nickname);
}
