package joat.user.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Follow 엔티티의 복합키 클래스.
 * follows 테이블의 (follower_id, following_id) 조합을 기본키로 사용한다.
 * Serializable 구현은 JPA 복합키 스펙 요구사항이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FollowId implements Serializable {
    private UUID followerId;   // 팔로우 요청자 UUID
    private UUID followingId;  // 팔로우 대상 UUID
}
