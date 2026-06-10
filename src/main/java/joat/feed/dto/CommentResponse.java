package joat.feed.dto;

import joat.feed.entity.Comment;
import joat.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 댓글 응답 DTO.
 * GET /api/posts/{postId}/comments, POST /api/posts/{postId}/comments 응답에 사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    /** 댓글 UUID */
    private UUID id;
    /** 작성자 UUID */
    private UUID userId;
    /** 부모 댓글 UUID (대댓글이면 non-null, 일반 댓글이면 null) */
    private UUID parentId;
    /** 댓글 본문 */
    private String content;
    /** 댓글 작성 시각 */
    private LocalDateTime createdAt;
    /** 작성자 요약 정보 */
    private AuthorInfo author;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthorInfo {
        /** 작성자 UUID */
        private UUID id;
        /** 작성자 닉네임 */
        private String nickname;
        /** 작성자 프로필 이미지 URL */
        private String profileImageUrl;

        public static AuthorInfo from(User user) {
            return AuthorInfo.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();
        }
    }

    public static CommentResponse from(Comment comment, User author) {
        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .author(author != null ? AuthorInfo.from(author) : null)
                .build();
    }
}
