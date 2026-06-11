package joat.feed.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.feed.dto.CreatePostRequest;
import joat.feed.dto.PostResponse;
import joat.feed.entity.Post;
import joat.feed.entity.PostType;
import joat.feed.repository.LikeRepository;
import joat.feed.repository.PostRepository;
import joat.tag.repository.PostTagRepository;
import joat.tag.repository.TagRepository;
import joat.tag.service.TagService;
import joat.todo.entity.Todo;
import joat.todo.entity.TodoItem;
import joat.todo.service.TodoService;
import joat.user.entity.OAuthProvider;
import joat.user.entity.User;
import joat.user.repository.FollowRepository;
import joat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * PostServiceImpl 단위 테스트.
 *
 * 전체 포스트 작성/조회/삭제/좋아요 플로우:
 *   1. createPost — userId + CreatePostRequest → Post 저장 → PostResponse 반환
 *      - todoId 없음 → Post.free(type=free)
 *      - todoId 있음 → Post.todoCert(type=todo_cert, todoId 연결)
 *   2. getPost — postId → Post 조회 → author(User) + tags + todo(TodoSummary) 조합 → PostResponse 반환
 *      - TodoSummary: TodoService.findTodo()로 투두 단건 조회 후 title + items 변환
 *   3. deletePost — postId + requesterId → 소유자 검증 → 삭제
 *   4. like/unlike — postId + userId → 중복/미좋아요 검증 → likes 테이블 조작
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks PostServiceImpl postService;

    @Mock PostRepository postRepository;
    @Mock LikeRepository likeRepository;
    @Mock FollowRepository followRepository;
    @Mock UserRepository userRepository;
    @Mock PostTagRepository postTagRepository;
    @Mock TagRepository tagRepository;
    @Mock TodoService todoService;
    @Mock PostSaveHelper postSaveHelper;
    @Mock TagService tagService;
    // Optional<PostEventProducer>는 Mockito가 직접 주입할 수 없어 null로 들어감
    // — tagNames 없는 createPost 테스트에서는 해당 분기 미진입이므로 문제없음

    // ── createPost ──────────────────────────────────────────────────

    @Test
    void createPost_todoId_없으면_FREE_타입으로_저장된다() {
        // 자유글: todoId가 null이면 Post.free()로 생성 → type=free
        UUID userId = UUID.randomUUID();
        CreatePostRequest req = new CreatePostRequest("오늘 갓생 살았다", null, null, null, null);
        User author = User.of("갓생러", OAuthProvider.kakao, "kakao-1");
        given(postSaveHelper.saveAndCommit(any())).willAnswer(inv -> inv.getArgument(0));
        given(userRepository.findById(userId)).willReturn(Optional.of(author));

        PostResponse res = postService.createPost(userId, req);

        assertThat(res.getType()).isEqualTo(PostType.free);
        assertThat(res.getAuthor()).isNotNull();
        assertThat(res.getAuthor().getNickname()).isEqualTo("갓생러");
        then(postSaveHelper).should().saveAndCommit(any(Post.class));
    }

    @Test
    void createPost_todoId_있으면_TODO_CERT_타입으로_저장된다() {
        // 투두 인증: todoId가 있으면 Post.todoCert()로 생성 → type=todo_cert, todoId 연결
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        CreatePostRequest req = new CreatePostRequest("투두 완료!", null, null, todoId, null);
        User author = User.of("갓생러", OAuthProvider.kakao, "kakao-1");
        given(postSaveHelper.saveAndCommit(any())).willAnswer(inv -> inv.getArgument(0));
        given(userRepository.findById(userId)).willReturn(Optional.of(author));

        PostResponse res = postService.createPost(userId, req);

        assertThat(res.getType()).isEqualTo(PostType.todo_cert);
        assertThat(res.getTodoId()).isEqualTo(todoId);
        assertThat(res.getAuthor()).isNotNull();
    }

    // ── getPost ─────────────────────────────────────────────────────

    @Test
    void getPost_투두_인증_포스트_조회시_TodoSummary가_응답에_포함된다() {
        // 조회 플로우: Post → author(User) → tags(빈 리스트) → Todo → TodoSummary 변환
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        Post post = Post.todoCert(userId, "투두 인증 게시글", null, todoId);
        ReflectionTestUtils.setField(post, "id", postId); // JPA 없이 ID 직접 주입
        User author = User.of("갓생러", OAuthProvider.kakao, "kakao-1");

        // 투두와 항목 생성 (items는 ArrayList이므로 직접 add 가능)
        Todo todo = Todo.of(userId, "오늘의 갓생 플랜", true, LocalDate.now());
        todo.getItems().add(TodoItem.of(todo, "운동 30분", 0));
        todo.getItems().add(TodoItem.of(todo, "알고리즘 1문제", 1));

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(userRepository.findById(userId)).willReturn(Optional.of(author));
        given(postTagRepository.findByPostIdIn(any())).willReturn(List.of());
        given(todoService.findTodo(todoId)).willReturn(todo);

        PostResponse res = postService.getPost(postId);

        // TodoSummary가 포함되어야 함
        assertThat(res.getTodo()).isNotNull();
        assertThat(res.getTodo().getTitle()).isEqualTo("오늘의 갓생 플랜");
        assertThat(res.getTodo().getItems()).hasSize(2);
        assertThat(res.getTodo().getItems().get(0).getContent()).isEqualTo("운동 30분");
        assertThat(res.getTodo().getItems().get(1).getContent()).isEqualTo("알고리즘 1문제");

        // author 정보도 포함되어야 함
        assertThat(res.getAuthor()).isNotNull();
        assertThat(res.getAuthor().getNickname()).isEqualTo("갓생러");
    }

    @Test
    void getPost_자유글_조회시_todo_필드는_null이다() {
        // 자유글은 todoId가 없으므로 TodoService를 호출하지 않고 todo=null 반환
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        Post post = Post.free(userId, "자유 게시글", null);
        ReflectionTestUtils.setField(post, "id", postId);
        User author = User.of("갓생러", OAuthProvider.kakao, "kakao-1");

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(userRepository.findById(userId)).willReturn(Optional.of(author));
        given(postTagRepository.findByPostIdIn(any())).willReturn(List.of());

        PostResponse res = postService.getPost(postId);

        assertThat(res.getTodo()).isNull();
        assertThat(res.getTags()).isEmpty();
        then(todoService).should(never()).findTodo(any());
    }

    @Test
    void getPost_없는_포스트_조회시_POST_NOT_FOUND_예외가_발생한다() {
        UUID postId = UUID.randomUUID();
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(postId))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void getPost_투두가_삭제된_경우_todo_필드는_null이다() {
        // 투두가 이미 삭제됐어도 포스트 조회는 성공해야 함 (graceful degradation)
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        Post post = Post.todoCert(userId, "투두 인증", null, todoId);
        ReflectionTestUtils.setField(post, "id", postId);
        User author = User.of("갓생러", OAuthProvider.kakao, "kakao-1");

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(userRepository.findById(userId)).willReturn(Optional.of(author));
        given(postTagRepository.findByPostIdIn(any())).willReturn(List.of());
        given(todoService.findTodo(todoId))
            .willThrow(new BusinessException(ErrorCode.TODO_NOT_FOUND));

        PostResponse res = postService.getPost(postId);

        // 투두를 못 찾아도 포스트 자체는 정상 반환, todo만 null
        assertThat(res).isNotNull();
        assertThat(res.getTodo()).isNull();
    }

    // ── deletePost ──────────────────────────────────────────────────

    @Test
    void 다른_유저_게시물_삭제시_POST_ACCESS_DENIED_예외가_발생한다() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Post post = Post.free(owner, "갓생 인증", null);
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(post.getId(), other))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.POST_ACCESS_DENIED);
    }

    // ── like / unlike ───────────────────────────────────────────────

    @Test
    void 이미_좋아요한_게시물_좋아요시_ALREADY_LIKED_예외가_발생한다() {
        UUID userId = UUID.randomUUID();
        Post post = Post.free(userId, "갓생", null);
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(likeRepository.existsByUserIdAndPostId(userId, post.getId())).willReturn(true);

        assertThatThrownBy(() -> postService.like(post.getId(), userId))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.ALREADY_LIKED);
    }

    @Test
    void 좋아요하지_않은_게시물_취소시_NOT_LIKED_예외가_발생한다() {
        UUID userId = UUID.randomUUID();
        Post post = Post.free(userId, "갓생", null);
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(likeRepository.existsByUserIdAndPostId(userId, post.getId())).willReturn(false);

        assertThatThrownBy(() -> postService.unlike(post.getId(), userId))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.NOT_LIKED);
    }

    @Test
    void 좋아요하면_likeCount가_1_증가한다() {
        UUID userId = UUID.randomUUID();
        Post post = Post.free(userId, "갓생", null);
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(likeRepository.existsByUserIdAndPostId(userId, post.getId())).willReturn(false);

        postService.like(post.getId(), userId);

        assertThat(post.getLikeCount()).isEqualTo(1);
    }

    @Test
    void 좋아요취소하면_likeCount가_1_감소한다() {
        UUID userId = UUID.randomUUID();
        Post post = Post.free(userId, "갓생", null);
        post.incrementLike(); // likeCount = 1
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(likeRepository.existsByUserIdAndPostId(userId, post.getId())).willReturn(true);

        postService.unlike(post.getId(), userId);

        assertThat(post.getLikeCount()).isEqualTo(0);
    }
}
