package joat.tag.repository;

import joat.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    // 태그명으로 단건 조회 (TagServiceImpl.processTags에서 upsert 패턴에 사용)
    Optional<Tag> findByName(String name);
    // prefix로 시작하는 태그 검색 — 해시태그 자동완성 UI에서 사용 (대소문자 무시)
    List<Tag> findByNameStartingWithIgnoreCase(String prefix);
    // 게시물 수 기준 상위 20개 조회 — Redis 캐시 미스 시 DB fallback에 사용
    List<Tag> findTop20ByOrderByPostCountDesc();
}
