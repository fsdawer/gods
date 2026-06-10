package joat.feed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import joat.common.entity.BaseEntity;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // UUID v7 기반 기본키

    @Column(nullable = false)
    private UUID postId; // 댓글이 달린 게시물 UUID

    @Column(nullable = false)
    private UUID userId; // 댓글 작성자 UUID

    private UUID parentId; // 대댓글이면 부모 댓글 UUID, 일반 댓글이면 null

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 댓글 본문 텍스트

    public static Comment of(UUID postId, UUID userId, UUID parentId, String content) {
        Comment comment = new Comment();
        comment.postId = postId;
        comment.userId = userId;
        comment.parentId = parentId;
        comment.content = content;
        return comment;
    }

    public void validateOwner(UUID userId) {
        if (!this.userId.equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_ACCESS_DENIED);
        }
    }
}
