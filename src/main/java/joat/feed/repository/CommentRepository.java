package joat.feed.repository;

import joat.feed.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    // 첫 페이지 — postId 기준 시간 오름차순 (커서 없음)
    Slice<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);
    // 커서 이후 페이지 — cursor UUID의 createdAt보다 이후인 댓글만 조회
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId AND c.createdAt > (SELECT cc.createdAt FROM Comment cc WHERE cc.id = :cursor) ORDER BY c.createdAt ASC")
    Slice<Comment> findByPostIdAfterCursor(@Param("postId") UUID postId, @Param("cursor") UUID cursor, Pageable pageable);
}
