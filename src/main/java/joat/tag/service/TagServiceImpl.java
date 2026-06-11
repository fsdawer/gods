package joat.tag.service;

import joat.tag.entity.PostTag;
import joat.tag.entity.PostTagId;
import joat.tag.entity.Tag;
import joat.tag.dto.TagResponse;
import joat.tag.repository.PostTagRepository;
import joat.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TagService 구현체.
 * 태그 처리(동기 직접 호출 또는 Kafka 재처리), Redis Sorted Set 기반 트렌딩, DB 기반 검색을 제공한다.
 * markTagDone 호출은 호출자(PostServiceImpl·TagEventConsumer·TagRetryBatch)가 담당한다.
 * PostService 의존을 제거해 PostServiceImpl↔TagServiceImpl 순환 참조를 차단한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    /** Redis Sorted Set 키: 값=태그명, score=누적 사용 횟수 */
    private static final String TRENDING_KEY = "tags:trending";

    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * [태그 처리 플로우 — PostServiceImpl(동기), TagEventConsumer(Kafka), TagRetryBatch(배치)에서 호출]
     *
     * 태그명 목록을 순회하며:
     * 1. 소문자 정규화 (normalized = name.toLowerCase())
     * 2. tags 테이블에서 name으로 조회 — 없으면 신규 Tag 생성/저장
     * 3. (postId, tagId) 조합이 이미 존재하면 건너뜀 (멱등성 — 재시도·배치 중복 방지)
     * 4. post_tags 테이블에 (postId, tagId) 삽입
     * 5. Tag.postCount + 1 (JPA dirty checking)
     * 6. Redis "tags:trending" Sorted Set에 해당 태그의 score += 1
     *
     * markTagDone은 호출자가 성공 후 별도로 호출한다.
     *
     * @param postId   태그를 붙일 포스트 UUID (호출 전 DB에 커밋된 상태여야 FK 제약 통과)
     * @param tagNames 태그명 목록
     */
    @Override
    @Transactional
    public void processTags(UUID postId, List<String> tagNames) {
        for (String name : tagNames) {
            String normalized = name.toLowerCase();

            Tag tag = tagRepository.findByName(normalized)
                .orElseGet(() -> tagRepository.save(Tag.of(normalized)));

            // 멱등성: 이미 연결된 태그는 건너뜀 (재시도 또는 배치 재처리 시 중복 방지)
            if (postTagRepository.existsById(new PostTagId(postId, tag.getId()))) continue;

            postTagRepository.save(PostTag.of(postId, tag.getId()));
            tag.incrementPostCount();
            redisTemplate.opsForZSet().incrementScore(TRENDING_KEY, normalized, 1);
        }
    }

    /**
     * [태그 검색 플로우 — prefix 자동완성]
     * tags 테이블에서 name LIKE 'query%' (대소문자 무시) 조건으로 조회한다.
     * 앱의 해시태그 입력 시 자동완성 UI에서 사용.
     *
     * @param query 검색어 (예: "갓생")
     * @return TagResponse 목록 (id, name, postCount)
     */
    @Override
    public List<TagResponse> search(String query) {
        return tagRepository.findByNameStartingWithIgnoreCase(query)
            .stream()
            .map(TagResponse::from)
            .toList();
    }

    /**
     * [트렌딩 태그 조회 플로우]
     * Redis Sorted Set을 우선 조회하여 응답속도를 높인다.
     *
     * 1. Redis "tags:trending" ZSet에서 상위 20개 태그명 조회 (score 내림차순)
     * 2. Redis 캐시가 비어있으면 (초기 상태 or TTL 만료) DB fallback:
     *    → tags 테이블에서 post_count DESC 상위 20개 조회
     * 3. 태그명 목록으로 DB 배치 조회 (WHERE name IN) → Redis 순서 유지하며 매핑
     *    (Redis에 있지만 DB에 없는 경우 null 필터)
     *
     * @return TagResponse 목록 (최대 20개, score/postCount 내림차순)
     */
    @Override
    public List<TagResponse> getTrending() {
        Set<Object> cached = redisTemplate.opsForZSet().reverseRange(TRENDING_KEY, 0, 19);

        if (cached != null && !cached.isEmpty()) {
            List<String> names = cached.stream().map(Object::toString).toList();
            // WHERE name IN (...) 한 번 실행 — 태그 20개마다 개별 조회하던 N+1 제거
            Map<String, Tag> tagMap = tagRepository.findAllByNameIn(names).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));
            return names.stream()
                .map(tagMap::get)
                .filter(Objects::nonNull)
                .map(TagResponse::from)
                .toList();
        }

        return tagRepository.findTop20ByOrderByPostCountDesc()
            .stream()
            .map(TagResponse::from)
            .toList();
    }
}
