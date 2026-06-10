package joat.feed.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Like 엔티티의 복합키 클래스.
 * likes 테이블의 (user_id, post_id) 조합을 기본키로 사용한다.
 * Serializable 구현은 JPA 복합키 스펙 요구사항이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LikeId implements Serializable {
    private UUID userId;  // 좋아요를 누른 유저 UUID
    private UUID postId;  // 좋아요 대상 게시물 UUID
}
