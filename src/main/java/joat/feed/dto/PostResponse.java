package joat.feed.dto;

import joat.feed.entity.Post;
import joat.feed.entity.PostType;
import joat.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 게시물 응답 DTO.
 * 피드/탐색/단건 조회 응답에 사용된다. author, tags, todo를 한 번에 내려줘
 * 앱이 추가 API 호출 없이 포스트 카드를 렌더링할 수 있다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
    /** 게시물 UUID */
    private UUID id;
    /** 작성자 UUID */
    private UUID userId;
    /** 게시물 유형 (free / todo_cert) */
    private PostType type;
    /** 본문 텍스트 */
    private String content;
    /** 첨부 이미지 URL 목록 */
    private List<String> imageUrls;
    /** 투두 인증 포스트일 때 연결된 투두 UUID */
    private UUID todoId;
    /** 좋아요 수 */
    private int likeCount;
    /** 댓글 수 */
    private int commentCount;
    /** 게시물 생성 시각 */
    private LocalDateTime createdAt;
    /** 작성자 요약 정보 (nickname, profileImageUrl) */
    private AuthorInfo author;
    /** 해시태그 이름 목록 */
    private List<String> tags;
    /** 투두 인증 포스트일 때 첨부된 투두 요약 (일반 포스트는 null) */
    private TodoSummary todo;



    /**
     * 투두 인증 포스트에 첨부되는 투두 요약 정보.
     *
     * 프론트엔드 렌더링 플로우:
     *   PostCard / PostDetailScreen → post.todo != null 이면
     *   → todo.title 표시 → todo.items 순회하며 완료 여부(✅/⬜) + 내용 표시
     *
     * completed 는 TodoItem.isDone 을 그대로 매핑. 이름은 프론트엔드 관례에 맞춰 completed.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TodoSummary {
        /** 투두 UUID */
        private UUID id;
        /** 투두 제목 */
        private String title;
        /** 투두 항목 목록 */
        private List<ItemInfo> items;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ItemInfo {
            /** 항목 UUID */
            private UUID id;
            /** 항목 내용 */
            private String content;
            /** 완료 여부 (TodoItem.isDone을 completed로 변환) */
            private boolean completed;
        }
    }

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

    public static PostResponse from(Post post, User author, List<String> tags, TodoSummary todo) {
        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .type(post.getType())
                .content(post.getContent())
                .imageUrls(Arrays.asList(post.getImageUrls()))
                .todoId(post.getTodoId())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .author(author != null ? AuthorInfo.from(author) : null)
                .tags(tags != null ? tags : List.of())
                .todo(todo)
                .build();
    }

    public static PostResponse from(Post post, User author, List<String> tags) {
        return from(post, author, tags, null);
    }

    public static PostResponse from(Post post) {
        return from(post, null, List.of(), null);
    }
}
