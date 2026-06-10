package joat.feed.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.feed.entity.Comment;
import joat.feed.entity.Post;
import joat.feed.dto.CommentResponse;
import joat.feed.dto.CreateCommentRequest;
import joat.feed.repository.CommentRepository;
import joat.feed.repository.PostRepository;
import joat.user.entity.User;
import joat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CommentService 구현체.
 * 댓글/대댓글 조회, 생성, 삭제 및 게시물 commentCount 카운터 동기화를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * [댓글 목록 조회 플로우]
     * 1. 게시물 ID로 댓글 전체 조회 (생성 시각 오름차순)
     * 2. 댓글이 없으면 빈 리스트 즉시 반환
     * 3. 작성자 UUID 목록으로 유저 배치 조회 (N+1 방지)
     * 4. userMap을 통해 각 댓글에 author 정보를 연결하여 반환
     */
    @Override
    public List<CommentResponse> getComments(UUID postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        if (comments.isEmpty()) return List.of();

        // author 배치 로딩 (N+1 방지)
        List<UUID> userIds = comments.stream().map(Comment::getUserId).distinct().toList();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream()
                .map(c -> CommentResponse.from(c, userMap.get(c.getUserId())))
                .toList();
    }

    /**
     * [댓글 작성 플로우]
     * 1. 게시물 존재 확인 (POST_NOT_FOUND)
     * 2. Comment 엔티티 저장 (parentId null이면 댓글, non-null이면 대댓글)
     * 3. Post.commentCount + 1 (JPA dirty checking)
     * 4. author 정보를 포함한 CommentResponse 반환
     */
    @Override
    @Transactional
    public CommentResponse createComment(UUID postId, UUID userId, CreateCommentRequest req) {
        Post post = findPost(postId);

        Comment comment = commentRepository.save(
            Comment.of(postId, userId, req.getParentId(), req.getContent())
        );

        // 게시물 댓글 수 카운터 증가
        post.incrementComment();

        User author = userRepository.findById(userId).orElse(null);
        return CommentResponse.from(comment, author);
    }

    /**
     * [댓글 삭제 플로우]
     * 1. 게시물 존재 확인 (POST_NOT_FOUND)
     * 2. 댓글 존재 확인 (COMMENT_NOT_FOUND)
     * 3. 소유자 검증 (COMMENT_ACCESS_DENIED)
     * 4. 댓글 삭제 + Post.commentCount - 1
     */
    @Override
    @Transactional
    public void deleteComment(UUID postId, UUID commentId, UUID userId) {
        Post post = findPost(postId);

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        comment.validateOwner(userId); // 본인 댓글이 아니면 COMMENT_ACCESS_DENIED

        commentRepository.delete(comment);
        // 게시물 댓글 수 카운터 감소
        post.decrementComment();
    }

    /**
     * 게시물 조회 공통 메서드 (없으면 POST_NOT_FOUND).
     */
    private Post findPost(UUID postId) {
        return postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }
}
