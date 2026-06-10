package joat.tag.controller;

import joat.common.response.ApiResponse;
import joat.tag.dto.TagResponse;
import joat.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 태그 관련 API 컨트롤러.
 * 해시태그 자동완성 검색과 트렌딩 태그 조회를 제공한다.
 * SecurityConfig에서 GET /api/tags/** 는 인증 없이 접근 가능하다.
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * [엔드포인트] GET /api/tags/search?q={query} — 해시태그 자동완성 검색
     * 입력 prefix로 시작하는 태그 목록을 반환한다 (앱의 해시태그 입력 UI에서 사용).
     */
    @GetMapping("/search")
    public ApiResponse<List<TagResponse>> search(@RequestParam String q) {
        return ApiResponse.ok(tagService.search(q));
    }

    /**
     * [엔드포인트] GET /api/tags/trending — 트렌딩 태그 상위 20개 조회
     * Redis Sorted Set 우선 조회, 캐시 미스 시 DB fallback으로 동작한다.
     */
    @GetMapping("/trending")
    public ApiResponse<List<TagResponse>> trending() {
        return ApiResponse.ok(tagService.getTrending());
    }
}
