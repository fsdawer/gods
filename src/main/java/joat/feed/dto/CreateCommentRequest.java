package joat.feed.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 댓글/대댓글 작성 요청 DTO.
 * POST /api/posts/{postId}/comments 에서 사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {
    @NotBlank
    /** 댓글 본문 (필수) */
    private String content;
    /** 대댓글일 때 부모 댓글 UUID, 일반 댓글이면 null */
    private UUID parentId;
}
