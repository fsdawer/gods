package joat.tag.repository;

import joat.tag.entity.PostTag;
import joat.tag.entity.PostTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostTagRepository extends JpaRepository<PostTag, PostTagId> {
    // 특정 게시물에 연결된 모든 태그 연결 행 조회
    List<PostTag> findByPostId(UUID postId);
    // 여러 게시물에 연결된 태그 연결 행을 한 번에 조회 (N+1 방지 배치 로딩)
    List<PostTag> findByPostIdIn(List<UUID> postIds);
}
