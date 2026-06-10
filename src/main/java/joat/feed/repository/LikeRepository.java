package joat.feed.repository;

import joat.feed.entity.Like;
import joat.feed.entity.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, LikeId> {
    // 특정 유저가 특정 게시물에 좋아요했는지 확인 (중복 좋아요 방지 및 취소 검증)
    boolean existsByUserIdAndPostId(UUID userId, UUID postId);
}
