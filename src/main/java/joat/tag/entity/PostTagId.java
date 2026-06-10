package joat.tag.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * PostTag 엔티티의 복합키 클래스.
 * post_tags 테이블의 (post_id, tag_id) 조합을 기본키로 사용한다.
 * Serializable 구현은 JPA 복합키 스펙 요구사항이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PostTagId implements Serializable {
    private UUID postId; // 게시물 UUID
    private UUID tagId;  // 태그 UUID
}
