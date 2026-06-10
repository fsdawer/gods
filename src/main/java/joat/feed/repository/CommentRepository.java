package joat.feed.repository;

import joat.feed.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    // 특정 게시물의 댓글을 시간 오름차순으로 전체 조회 (댓글/대댓글 포함)
    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);
}
