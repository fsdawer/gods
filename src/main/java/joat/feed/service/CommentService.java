package joat.feed.service;

import joat.feed.dto.CommentResponse;
import joat.feed.dto.CreateCommentRequest;
import joat.feed.dto.CursorResponse;

import java.util.UUID;

/**
 * 댓글 서비스 인터페이스.
 * 댓글/대댓글 조회, 생성, 삭제를 담당한다.
 */
public interface CommentService {

    /**
     * 포스트의 댓글 목록 조회 (커서 기반 페이지네이션, 시간 오름차순).
     *
     * @param postId 대상 포스트 UUID
     * @param cursor 이전 페이지 마지막 댓글 UUID (없으면 첫 페이지)
     * @param limit  페이지당 댓글 수
     * @return CursorResponse (data, nextCursor, hasNext)
     */
    CursorResponse<CommentResponse> getComments(UUID postId, UUID cursor, int limit);

    /**
     * 댓글 또는 대댓글 작성.
     *
     * @param postId 대상 포스트 UUID
     * @param userId 작성자 UUID
     * @param req    내용, parentId (대댓글이면 부모 댓글 UUID, 댓글이면 null)
     * @return 작성된 CommentResponse
     */
    CommentResponse createComment(UUID postId, UUID userId, CreateCommentRequest req);

    /**
     * 댓글 삭제 (본인 댓글만 가능).
     *
     * @param postId      대상 포스트 UUID
     * @param commentId   삭제할 댓글 UUID
     * @param userId 요청자 UUID (소유자 검증용)
     */
    void deleteComment(UUID postId, UUID commentId, UUID userId);
}
