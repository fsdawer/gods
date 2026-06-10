package joat.feed.service;

import joat.feed.dto.CommentResponse;
import joat.feed.dto.CreateCommentRequest;

import java.util.List;
import java.util.UUID;

/**
 * 댓글 서비스 인터페이스.
 * 댓글/대댓글 조회, 생성, 삭제를 담당한다.
 */
public interface CommentService {

    /**
     * 포스트의 댓글 목록 조회 (대댓글 포함, 시간순).
     *
     * @param postId 대상 포스트 UUID
     * @return CommentResponse 목록 (parentId null=댓글, non-null=대댓글)
     */
    List<CommentResponse> getComments(UUID postId);

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
