package joat.todo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 투두 인증 포스트 작성 요청 DTO.
 * POST /api/todos/{todoId}/certify 에서 사용된다.
 * 내부적으로 CreatePostRequest로 변환되어 PostService.createPost를 호출한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CertifyRequest {
    /** 인증 포스트 본문 (필수) */
    @NotBlank(message = "인증 포스트 본문은 비어 있을 수 없습니다.")
    private String content;
    /** 첨부할 이미지 S3 URL 목록 (없으면 null) */
    private List<String> imageUrls;
    /** 해시태그 이름 목록 (없으면 null) */
    private List<String> tagNames;
}
