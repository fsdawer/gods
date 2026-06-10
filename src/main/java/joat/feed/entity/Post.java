package joat.feed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // UUID v7 기반 기본키

    @Column(nullable = false)
    private UUID userId; // 게시물 작성자 UUID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type; // 게시물 유형 (free=일반, todo_cert=투두 인증)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 게시물 본문 텍스트

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]")
    private String[] imageUrls = new String[0]; // 첨부 이미지 S3 URL 배열 (없으면 빈 배열)

    private UUID todoId; // 투두 인증 포스트일 때 연결된 투두 UUID (일반 포스트는 null)

    private UUID teamId; // 팀방 전용 게시물일 때 소속 팀방 UUID (공개 게시물은 null)

    @Column(nullable = false)
    private int likeCount; // 좋아요 수 (카운터 캐시, likes 테이블 집계 대신 사용)

    @Column(nullable = false)
    private int commentCount; // 댓글 수 (카운터 캐시, comments 테이블 집계 대신 사용)

    public static Post free(UUID userId, String content, String[] imageUrls) {
        Post post = new Post();
        post.userId = userId;
        post.type = PostType.free;
        post.content = content;
        post.imageUrls = imageUrls != null ? imageUrls : new String[0];
        return post;
    }

    public static Post todoCert(UUID userId, String content, String[] imageUrls, UUID todoId) {
        Post post = free(userId, content, imageUrls);
        post.type = PostType.todo_cert;
        post.todoId = todoId;
        return post;
    }

    public void validateOwner(UUID userId) {
        if (!this.userId.equals(userId)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }
    }

    public void update(String content, String[] imageUrls) {
        if (content != null) this.content = content;
        if (imageUrls != null) this.imageUrls = imageUrls;
    }

    public void incrementLike() { this.likeCount++; }
    public void decrementLike() { this.likeCount = Math.max(0, this.likeCount - 1); }
    public void incrementComment() { this.commentCount++; }
    public void decrementComment() { this.commentCount = Math.max(0, this.commentCount - 1); }
}
