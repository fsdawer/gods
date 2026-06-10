package joat.tag.dto;

import joat.tag.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 태그 응답 DTO.
 * GET /api/tags/search, GET /api/tags/trending 응답에 사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagResponse {
    /** 태그 UUID */
    private UUID id;
    /** 소문자 정규화된 태그명 */
    private String name;
    /** 이 태그가 사용된 게시물 수 */
    private int postCount;

    public static TagResponse from(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .postCount(tag.getPostCount())
                .build();
    }
}
