package joat.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 게시물 수정 요청 DTO.
 * PATCH /api/posts/{postId} 에서 사용된다.
 * null 필드는 기존값을 유지한다. tags는 Kafka를 통해 비동기 재처리한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {
    /** 수정할 본문 텍스트 (null이면 기존값 유지) */
    private String content;
    /** 수정할 이미지 URL 목록 (null이면 기존값 유지) */
    private List<String> imageUrls;
    /** 수정할 해시태그 목록 (null이면 태그 변경 없음) */
    private List<String> tagNames;
}
