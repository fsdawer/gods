package joat.user.entity;

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
@Table(name = "follows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(FollowId.class)
public class Follow {

    @Id
    @Column(name = "follower_id")
    private UUID followerId; // 팔로우를 요청한 유저 UUID

    @Id
    @Column(name = "following_id")
    private UUID followingId; // 팔로우 대상 유저 UUID

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 팔로우 생성 시각 (수정 불가)

    public static Follow of(UUID followerId, UUID followingId) {
        Follow follow = new Follow();
        follow.followerId = followerId;
        follow.followingId = followingId;
        return follow;
    }
}
