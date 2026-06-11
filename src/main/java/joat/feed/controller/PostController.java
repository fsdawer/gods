package joat.feed.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import joat.common.response.ApiResponse;
import joat.feed.dto.CommentResponse;
import joat.feed.dto.CreateCommentRequest;
import joat.feed.dto.CreatePostRequest;
import joat.feed.dto.CursorResponse;
import joat.feed.dto.PostResponse;
import joat.feed.dto.UpdatePostRequest;
import joat.feed.service.CommentService;
import joat.feed.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 게시물 및 댓글 관련 API 컨트롤러.
 * 피드 조회, 포스트 CRUD, 좋아요, 댓글 CRUD를 하나의 컨트롤러에서 관리한다.
 */
@Validated
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    /**
     * [엔드포인트] POST /api/posts — 게시물 작성
     * todoId 포함 시 투두 인증 포스트, 없으면 일반 포스트로 생성된다.
     */
    @PostMapping
    public ApiResponse<PostResponse> create(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody CreatePostRequest req
    ) {
        return ApiResponse.ok(postService.createPost(userId, req));
    }

    /**
     * [엔드포인트] GET /api/posts — 홈 피드 조회 (팔로잉 기반, 커서 페이지네이션)
     * 내가 팔로우하는 유저 + 본인의 게시물을 최신순으로 반환한다.
     */
    @GetMapping
    public ApiResponse<CursorResponse<PostResponse>> feed(
        @AuthenticationPrincipal UUID userId,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(postService.getFeed(userId, limit));
    }

    /**
     * [엔드포인트] GET /api/posts/explore — 탐색/태그 피드 조회 (인증 불필요)
     *
     * - tag 없음: 전체 게시물 최신순 (커서 미지원, limit만 적용)
     * - tag 있음: 해당 태그가 달린 게시물만 커서 기반으로 반환
     *
     * @param limit  페이지당 게시물 수 (기본 20)
     * @param tag    태그 이름 필터 (소문자 자동 변환, 없으면 전체 조회)
     * @param cursor 이전 페이지 마지막 포스트 UUID (tag 있을 때만 유효)
     */
    @GetMapping("/explore")
    public ApiResponse<CursorResponse<PostResponse>> explore(
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) UUID cursor
    ) {
        if (tag != null && !tag.isBlank()) {
            return ApiResponse.ok(postService.getPostsByTag(tag.toLowerCase(), cursor, limit));
        }
        return ApiResponse.ok(postService.getExplore(limit));
    }

    /**
     * [엔드포인트] GET /api/posts/{postId} — 게시물 단건 조회
     * author, tags, todo 정보를 함께 반환한다.
     */
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> get(@PathVariable UUID postId) {
        return ApiResponse.ok(postService.getPost(postId));
    }

    /**
     * [엔드포인트] DELETE /api/posts/{postId} — 게시물 삭제
     * 본인 게시물이 아니면 POST_ACCESS_DENIED 예외가 발생한다.
     */
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID postId
    ) {
        postService.deletePost(postId, userId);
        return ApiResponse.ok();
    }

    /**
     * [엔드포인트] POST /api/posts/{postId}/like — 좋아요
     * 이미 좋아요한 경우 ALREADY_LIKED 예외가 발생한다.
     */
    @PostMapping("/{postId}/like")
    public ApiResponse<Void> like(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID postId
    ) {
        postService.like(postId, userId);
        return ApiResponse.ok();
    }

    /**
     * [엔드포인트] DELETE /api/posts/{postId}/like — 좋아요 취소
     * 좋아요하지 않은 경우 NOT_LIKED 예외가 발생한다.
     */
    @DeleteMapping("/{postId}/like")
    public ApiResponse<Void> unlike(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID postId
    ) {
        postService.unlike(postId, userId);
        return ApiResponse.ok();
    }

    /**
     * [엔드포인트] GET /api/posts/{postId}/comments — 댓글 목록 조회 (커서 기반, 시간 오름차순)
     *
     * @param cursor 이전 페이지 마지막 댓글 UUID (없으면 첫 페이지)
     * @param limit  페이지당 댓글 수 (기본 20, 최대 100)
     */
    @GetMapping("/{postId}/comments")
    public ApiResponse<CursorResponse<CommentResponse>> comments(
        @PathVariable UUID postId,
        @RequestParam(required = false) UUID cursor,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(commentService.getComments(postId, cursor, limit));
    }

    /**
     * [엔드포인트] POST /api/posts/{postId}/comments — 댓글/대댓글 작성
     * parentId 포함 시 대댓글, 없으면 일반 댓글로 생성된다.
     */
    @PostMapping("/{postId}/comments")
    public ApiResponse<CommentResponse> createComment(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID postId,
        @Valid @RequestBody CreateCommentRequest req
    ) {
        return ApiResponse.ok(commentService.createComment(postId, userId, req));
    }

    /**
     * [엔드포인트] PATCH /api/posts/{postId} — 게시물 수정
     * content, imageUrls, tagNames를 수정한다. null 필드는 기존값을 유지한다.
     * 본인 게시물이 아니면 POST_FORBIDDEN 예외가 발생한다.
     */
    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> update(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID postId,
        @Valid @RequestBody UpdatePostRequest req
    ) {
        return ApiResponse.ok(postService.updatePost(postId, userId, req));
    }

    /**
     * [엔드포인트] DELETE /api/posts/{postId}/comments/{commentId} — 댓글 삭제
     * 본인 댓글이 아니면 COMMENT_ACCESS_DENIED 예외가 발생한다.
     */
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID postId,
        @PathVariable UUID commentId
    ) {
        commentService.deleteComment(postId, commentId, userId);
        return ApiResponse.ok();
    }
}
