package joat.feed.repository;

import joat.feed.entity.Post;
import joat.feed.entity.TagStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    // 특정 유저 목록의 게시물을 최신순으로 조회 (홈 피드 — 팔로잉 + 본인 포스트)
    @Query("SELECT p FROM Post p WHERE p.userId IN :userIds ORDER BY p.createdAt DESC")
    Slice<Post> findFeed(@Param("userIds") List<UUID> userIds, Pageable pageable);

    // 전체 게시물을 최신순으로 조회 (탐색 피드 — 인증 불필요)
    Slice<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 특정 유저의 게시물 수 조회 (프로필 postCount용)
    long countByUserId(UUID userId);

    // 내 게시물 — 커서 없이 처음 조회 (최신순)
    @Query("SELECT p FROM Post p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
    Slice<Post> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    // 내 게시물 — 커서 이후 조회 (cursor UUID의 createdAt보다 이전인 것)
    @Query("SELECT p FROM Post p WHERE p.userId = :userId AND p.createdAt < (SELECT c.createdAt FROM Post c WHERE c.id = :cursor) ORDER BY p.createdAt DESC")
    Slice<Post> findByUserIdAfterCursor(@Param("userId") UUID userId, @Param("cursor") UUID cursor, Pageable pageable);

    // 특정 태그의 공개 게시물 — 커서 없이 처음 조회 (teamId IS NULL = 팀방 전용 제외)
    @Query("SELECT p FROM Post p WHERE p.teamId IS NULL AND p.id IN (SELECT pt.postId FROM PostTag pt WHERE pt.tagId = (SELECT t.id FROM Tag t WHERE t.name = :tagName)) ORDER BY p.createdAt DESC")
    Slice<Post> findByTagName(@Param("tagName") String tagName, Pageable pageable);

    // 특정 태그의 공개 게시물 — 커서 이후 조회 (teamId IS NULL = 팀방 전용 제외)
    @Query("SELECT p FROM Post p WHERE p.teamId IS NULL AND p.id IN (SELECT pt.postId FROM PostTag pt WHERE pt.tagId = (SELECT t.id FROM Tag t WHERE t.name = :tagName)) AND p.createdAt < (SELECT c.createdAt FROM Post c WHERE c.id = :cursor) ORDER BY p.createdAt DESC")
    Slice<Post> findByTagNameAfterCursor(@Param("tagName") String tagName, @Param("cursor") UUID cursor, Pageable pageable);

    // 팀 피드 — 유저 목록 기반, 커서 이후 조회
    @Query("SELECT p FROM Post p WHERE p.userId IN :userIds AND p.createdAt < (SELECT c.createdAt FROM Post c WHERE c.id = :cursor) ORDER BY p.createdAt DESC")
    Slice<Post> findFeedAfterCursor(@Param("userIds") List<UUID> userIds, @Param("cursor") UUID cursor, Pageable pageable);

    // 태그 처리 실패 포스트 조회 (배치 재처리용)
    List<Post> findByTagStatus(TagStatus tagStatus);
}
