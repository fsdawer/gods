# DB 개발 참조 문서

## 확정 스키마 테이블

```
users, posts, todos, todo_items, comments, tags, post_tags, follows, likes
```

전체 컬럼 정의: `docs/superpowers/specs/2026-05-31-gatsaeng-community-design.md`

## JPA 엔티티 위치

- `joat.feed.entity` — Post, Comment, Like, LikeId, PostType
- `joat.user.entity` — User, Follow, FollowId, OAuthProvider
- `joat.todo.entity` — Todo, TodoItem
- `joat.tag.entity` — Tag, PostTag, PostTagId
- `joat.common.entity` — BaseEntity (createdAt, updatedAt)

## 복합 PK 규칙

record 사용 금지. 다음 패턴으로 작성:

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FollowId implements Serializable {
    private UUID followerId;
    private UUID followingId;
}
```

엔티티에서 `@IdClass(FollowId.class)` 사용.

## 인덱스 전략

```sql
-- 피드 조회
CREATE INDEX idx_posts_user_created ON posts(user_id, created_at DESC);
CREATE INDEX idx_posts_created ON posts(created_at DESC);

-- 팔로우
CREATE INDEX idx_follows_follower ON follows(follower_id);
CREATE INDEX idx_follows_following ON follows(following_id);

-- 해시태그
CREATE INDEX idx_tags_name ON tags(name);
CREATE INDEX idx_post_tags_tag ON post_tags(tag_id);

-- 댓글
CREATE INDEX idx_comments_post ON comments(post_id, created_at ASC);

-- 투두
CREATE INDEX idx_todos_user_date ON todos(user_id, date DESC);
```

## Redis 캐시 키

| 키 | 내용 | TTL |
|---|---|---|
| `auth:refresh:{userId}` | Refresh Token | 14일 |
| `tags:trending` | Sorted Set (태그명:score) | 영구(score 기반) |
| `tag:trending` | 트렌딩 태그 목록 | 30분 |

## Flyway 규칙

- 파일명: `V{N}__{설명}.sql` (언더스코어 두 개)
- 위치: `src/main/resources/db/migration/`
- 배포된 파일 수정 금지 — 새 버전 파일 추가

## N+1 방지

- `@OneToMany`는 기본 LAZY
- 피드 조회처럼 N+1이 발생하는 쿼리는 `@EntityGraph` 또는 fetch join
- `like_count`, `comment_count`는 카운터 캐시 — COUNT 쿼리 금지
