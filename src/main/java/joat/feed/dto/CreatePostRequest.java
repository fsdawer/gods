package joat.feed.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 게시물 생성 요청 DTO.
 * POST /api/posts 또는 POST /api/teams/{teamId}/posts 에서 사용된다.
 * todoId가 있으면 투두 인증 포스트(todo_cert), teamId가 있으면 팀방 게시물,
 * 둘 다 없으면 일반 포스트(free)로 생성된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {
    @NotBlank
    /** 게시물 본문 (필수) */
    private String content;
    /** 첨부할 이미지 S3 URL 목록 (없으면 null 또는 빈 리스트) */
    private List<String> imageUrls;
    /** 해시태그 이름 목록 (없으면 null, Kafka로 비동기 처리) */
    private List<String> tagNames;
    /** 투두 인증 포스트일 때 연결할 투두 UUID (일반 포스트는 null) */
    private UUID todoId;
    /** 팀방 게시물일 때 소속 팀방 UUID (공개 게시물은 null) */
    private UUID teamId;
}
