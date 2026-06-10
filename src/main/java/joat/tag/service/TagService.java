package joat.tag.service;

import joat.tag.dto.TagResponse;

import java.util.List;
import java.util.UUID;

/**
 * 해시태그 서비스 인터페이스.
 * 태그 처리(Kafka 비동기), 검색, 트렌딩 조회를 담당한다.
 */
public interface TagService {

    /**
     * Kafka "post.created" 이벤트 수신 후 태그 처리.
     * tags 테이블에 없는 태그는 신규 생성, post_tags에 연결,
     * Redis Sorted Set("tags:trending")의 score를 증가시킨다.
     *
     * @param postId   태그를 붙일 포스트 UUID
     * @param tagNames 태그명 목록 (소문자 정규화 처리됨)
     */
    void processTags(UUID postId, List<String> tagNames);

    /**
     * 해시태그 자동완성 검색 (prefix 매칭).
     *
     * @param query 검색어 (예: "갓생" → "갓생루틴", "갓생챌린지" 반환)
     * @return TagResponse 목록
     */
    List<TagResponse> search(String query);

    /**
     * 트렌딩 태그 상위 20개 조회.
     * Redis Sorted Set 우선 조회 → 비어있으면 DB fallback.
     *
     * @return TagResponse 목록 (score 내림차순, 최대 20개)
     */
    List<TagResponse> getTrending();
}
