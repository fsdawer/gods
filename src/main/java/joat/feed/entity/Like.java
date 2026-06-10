package joat.feed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(LikeId.class)
public class Like {

    @Id
    private UUID userId; // 좋아요를 누른 유저 UUID

    @Id
    private UUID postId; // 좋아요 대상 게시물 UUID

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 좋아요 생성 시각 (수정 불가)

    public static Like of(UUID userId, UUID postId) {
        Like like = new Like();
        like.userId = userId;
        like.postId = postId;
        return like;
    }
}
