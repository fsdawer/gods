package joat.tag.service;

import joat.feed.service.PostService;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * TagService 구현체.
 * Kafka 이벤트로 비동기 태그 처리, Redis Sorted Set 기반 트렌딩, DB 기반 검색을 제공한다.
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
    private final PostService postService;

    /**
     * [태그 처리 플로우 — Kafka Consumer 또는 배치 재처리에서 호출]
     * TagEventConsumer.onPostCreated → 이 메서드 호출 (비동기, 트랜잭션 내)
     * TagRetryBatch → 이 메서드 호출 (FAILED 포스트 재처리)
     *
     * 태그명 목록을 순회하며:
     * 1. 소문자 정규화 (normalized = name.toLowerCase())
     * 2. tags 테이블에서 name으로 조회 — 없으면 신규 Tag 생성/저장
     * 3. (postId, tagId) 조합이 이미 존재하면 건너뜀 (멱등성 — 재시도/배치 중복 방지)
     * 4. post_tags 테이블에 (postId, tagId) 삽입
     * 5. Tag.postCount + 1 (JPA dirty checking)
     * 6. Redis "tags:trending" Sorted Set에 해당 태그의 score += 1
     * 7. 모든 태그 처리 완료 후 PostService.markTagDone(postId) 호출
     *
     * 입력: postId (연결할 포스트 UUID), tagNames (태그명 목록)
     * 호출: TagRepository.findByName → TagRepository.save → PostTagRepository.existsById
     *       → PostTagRepository.save → Tag.incrementPostCount
     *       → RedisTemplate.opsForZSet().incrementScore → PostService.markTagDone
     * 반환: void
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
        postService.markTagDone(postId);
    }

    /**
     * [태그 검색 플로우 — prefix 자동완성]
     * tags 테이블에서 name LIKE 'query%' (대소문자 무시) 조건으로 조회한다.
     * 앱의 해시태그 입력 시 자동완성 UI에서 사용.
     *
     * 입력: query (검색어, 예: "갓생")
     * 호출: TagRepository.findByNameStartingWithIgnoreCase(query)
     * 반환: TagResponse 목록 (id, name, postCount)
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
     * 3. 태그명으로 DB에서 Tag 엔티티 조회 (Redis에 있지만 DB에 없는 경우 null 필터)
     *
     * 입력: 없음
     * 호출: RedisTemplate.opsForZSet().reverseRange → (miss 시) TagRepository.findTop20ByOrderByPostCountDesc
     * 반환: TagResponse 목록 (최대 20개, score/postCount 내림차순)
     */
    @Override
    public List<TagResponse> getTrending() {
        Set<Object> cached = redisTemplate.opsForZSet().reverseRange(TRENDING_KEY, 0, 19);

        if (cached != null && !cached.isEmpty()) {
            return cached.stream()
                .map(name -> tagRepository.findByName(name.toString())
                    .map(TagResponse::from)
                    .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        }

        return tagRepository.findTop20ByOrderByPostCountDesc()
            .stream()
            .map(TagResponse::from)
            .toList();
    }
}
