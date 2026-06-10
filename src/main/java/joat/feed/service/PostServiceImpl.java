package joat.feed.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.common.kafka.PostEventProducer;
import joat.common.kafka.event.PostCreatedEvent;
import joat.feed.entity.Like;
import joat.feed.entity.LikeId;
import joat.feed.entity.Post;
import joat.feed.dto.CreatePostRequest;
import joat.feed.dto.CursorResponse;
import joat.feed.dto.PostResponse;
import joat.feed.dto.UpdatePostRequest;
import joat.feed.repository.LikeRepository;
import joat.feed.repository.PostRepository;
import joat.tag.entity.PostTag;
import joat.tag.entity.Tag;
import joat.tag.repository.PostTagRepository;
import joat.tag.repository.TagRepository;
import joat.todo.entity.Todo;
import joat.todo.entity.TodoItem;
import joat.todo.service.TodoService;
import joat.user.entity.Follow;
import joat.user.entity.User;
import joat.user.repository.FollowRepository;
import joat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PostService 구현체.
 * 포스트 CRUD, 좋아요, 커서 기반 피드 페이지네이션을 처리한다.
 * 해시태그 처리는 Kafka 이벤트로 비동기 위임한다 (TagEventConsumer가 수신).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final PostTagRepository postTagRepository;
    private final TagRepository tagRepository;
    private final TodoService todoService;
    private final Optional<PostEventProducer> postEventProducer;

    /**
     * [포스트 생성 플로우]*
     * 1. imageUrls를 String[]로 변환 (null이면 빈 배열)
     * 2. teamId / todoId 유무로 분기:
     *    - teamId 있음 → Post.teamPost(type=free, teamId 연결) — 공개 피드 제외
     *    - todoId 있음 → Post.todoCert(type=todo_cert, todoId 연결)
     *    - 둘 다 없음 → Post.free(type=free)
     * 3. posts 테이블에 저장
     * 4. tagNames가 있으면 Kafka "post.created" 토픽으로 이벤트 발행
     *    → TagEventConsumer가 비동기로 tags/post_tags 테이블 처리
     *
     * 입력: userId (작성자), req (content, imageUrls, tagNames, todoId, teamId)
     * 호출: PostRepository.save → PostEventProducer.publishPostCreated (태그 있을 때)
     * 반환: PostResponse (id, userId, type, content, imageUrls, todoId, teamId, likeCount, commentCount, createdAt)
     */
    @Override
    @Transactional
    public PostResponse createPost(UUID userId, CreatePostRequest req) {
        // null-safe 이미지 URL 배열 변환
        String[] images = req.getImageUrls() != null
            ? req.getImageUrls().toArray(new String[0])
            : new String[0];

        // 팀방 게시물 > 투두 인증 포스트 > 자유 포스트 순으로 분기
        Post post;
        if (req.getTeamId() != null) {
            post = Post.teamPost(userId, req.getContent(), images, req.getTeamId());
        } else if (req.getTodoId() != null) {
            post = Post.todoCert(userId, req.getContent(), images, req.getTodoId());
        } else {
            post = Post.free(userId, req.getContent(), images);
        }

        Post saved = postRepository.save(post);

        // 해시태그가 있으면 Kafka로 비동기 처리 위임
        if (req.getTagNames() != null && !req.getTagNames().isEmpty()) {
            postEventProducer.ifPresent(p ->
                p.publishPostCreated(new PostCreatedEvent(saved.getId(), userId, req.getTagNames()))
            );
        }

        // 생성 응답에도 author를 포함해 프론트엔드가 별도 조회 없이 렌더링할 수 있게 함
        User author = userRepository.findById(userId).orElse(null);
        return PostResponse.from(saved, author, List.of(), null);
    }

    /**
     * [홈 피드 조회 플로우]
     * 1. follows 테이블에서 내가 팔로우하는 유저 ID 목록 조회
     * 2. 내 userId도 포함 (내 포스트도 피드에 노출)
     * 3. 해당 유저들의 포스트를 최신순으로 limit+1개 조회
     * 4. limit+1번째가 있으면 hasNext=true, nextCursor=마지막 포스트 ID
     *
     * 입력: userId (요청자), limit (페이지 크기, 기본 20)
     * 호출: FollowRepository.findByFollowerId → PostRepository.findFeed
     * 반환: CursorResponse(data[], nextCursor UUID, hasNext)
     */
    @Override
    public CursorResponse<PostResponse> getFeed(UUID userId, int limit) {
        // 팔로잉 ID 목록 + 본인 포함
        List<UUID> followingIds = followRepository.findByFollowerId(userId)
            .stream()
            .map(Follow::getFollowingId)
            .toList();

        List<UUID> userIds = new ArrayList<>(followingIds);
        userIds.add(userId); // 내 포스트도 포함

        Slice<Post> slice = postRepository.findFeed(userIds, PageRequest.of(0, limit + 1));
        return buildCursor(slice, limit);
    }

    /**
     * [탐색 피드 조회 플로우]
     * 전체 포스트를 최신순으로 limit+1개 조회하여 커서 응답을 구성한다.
     * 인증 불필요 (SecurityConfig에서 permitAll 처리됨).
     *
     * 입력: limit (페이지 크기)
     * 호출: PostRepository.findAllByOrderByCreatedAtDesc
     * 반환: CursorResponse(data[], nextCursor UUID, hasNext)
     */
    @Override
    public CursorResponse<PostResponse> getExplore(int limit) {
        Slice<Post> slice = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit + 1));
        return buildCursor(slice, limit);
    }

    /**
     * [포스트 단건 조회 플로우]
     *
     * 앱에서 포스트 카드를 탭하면 PostDetailScreen이 이 API를 호출한다.
     * 응답 한 번에 author·tags·todo(투두 인증 포스트라면) 를 모두 내려보낸다.
     *
     * 1. postId → Post 조회 (없으면 POST_NOT_FOUND)
     * 2. post.userId → User 조회 (author 정보)
     * 3. postId → PostTag → Tag 이름 목록 (tags 배열)
     * 4. post.todoId 있으면 → Todo + TodoItem 목록 → TodoSummary 변환
     *    - 투두가 이미 삭제된 경우 todo=null 로 graceful degradation
     * 5. PostResponse.from(post, author, tags, todoSummary) 반환
     */
    @Override
    public PostResponse getPost(UUID postId) {
        Post post = findPost(postId);
        User author = userRepository.findById(post.getUserId()).orElse(null);
        List<String> tags = loadTagNames(List.of(post.getId())).getOrDefault(post.getId(), List.of());
        PostResponse.TodoSummary todoSummary = loadTodoSummary(post.getTodoId());
        return PostResponse.from(post, author, tags, todoSummary);
    }

    /**
     * [포스트 삭제 플로우]
     * 1. postId로 Post 엔티티 조회 (없으면 POST_NOT_FOUND)
     * 2. 소유자 검증 (userId != post.userId → POST_ACCESS_DENIED)
     * 3. posts 테이블에서 삭제 (cascade로 comments, likes, post_tags도 삭제)
     *
     * 입력: postId, userId (JWT에서 파싱)
     * 호출: PostRepository.findById → Post.validateOwner → PostRepository.delete
     * 반환: void
     */
    @Override
    @Transactional
    public void deletePost(UUID postId, UUID userId) {
        Post post = findPost(postId);
        post.validateOwner(userId); // 소유자가 아니면 POST_ACCESS_DENIED
        postRepository.delete(post);
    }

    /**
     * [좋아요 추가 플로우]
     * 1. postId로 Post 조회 (없으면 POST_NOT_FOUND)
     * 2. 이미 좋아요한 경우 ALREADY_LIKED 예외
     * 3. likes 테이블에 (userId, postId) 삽입
     * 4. post.likeCount + 1 (JPA dirty checking으로 UPDATE)
     *
     * 입력: postId, userId (좋아요 누르는 유저)
     * 호출: LikeRepository.existsByUserIdAndPostId → LikeRepository.save → Post.incrementLike
     * 반환: void
     */
    @Override
    @Transactional
    public void like(UUID postId, UUID userId) {
        Post post = findPost(postId);
        if (likeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }
        likeRepository.save(Like.of(userId, postId));
        post.incrementLike(); // likeCount 카운터 캐시 증가
    }

    /**
     * [좋아요 취소 플로우]
     * 1. postId로 Post 조회
     * 2. 좋아요하지 않은 경우 NOT_LIKED 예외
     * 3. likes 테이블에서 (userId, postId) 삭제
     * 4. post.likeCount - 1 (최소 0)
     *
     * 입력: postId, userId (취소 유저)
     * 호출: LikeRepository.existsByUserIdAndPostId → LikeRepository.deleteById → Post.decrementLike
     * 반환: void
     */
    @Override
    @Transactional
    public void unlike(UUID postId, UUID userId) {
        Post post = findPost(postId);
        if (!likeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new BusinessException(ErrorCode.NOT_LIKED);
        }
        likeRepository.deleteById(new LikeId(userId, postId));
        post.decrementLike();
    }

    /**
     * [태그 게시물 목록 조회 플로우]
     * 1. cursor가 null이면 처음부터, 있으면 해당 포스트 이후부터 조회
     * 2. post_tags → posts 조인 쿼리로 해당 태그가 달린 게시물만 필터링
     * 3. limit+1 개 조회 → hasNext 판단 → buildCursor로 응답 구성
     *
     * 입력: tagName (소문자), cursor, limit
     * 호출: PostRepository.findByTagName 또는 findByTagNameAfterCursor
     * 반환: CursorResponse
     */
    @Override
    public CursorResponse<PostResponse> getPostsByTag(String tagName, UUID cursor, int limit) {
        Slice<Post> slice = cursor == null
            ? postRepository.findByTagName(tagName, PageRequest.of(0, limit + 1))
            : postRepository.findByTagNameAfterCursor(tagName, cursor, PageRequest.of(0, limit + 1));
        return buildCursor(slice, limit);
    }

    /**
     * [게시물 수 조회]
     * 프로필 통계(postCount)에 사용된다.
     *
     * 입력: userId
     * 호출: PostRepository.countByUserId
     * 반환: 게시물 수 (long)
     */
    @Override
    public long countPosts(UUID userId) {
        return postRepository.countByUserId(userId);
    }

    /**
     * [내 게시물 목록 조회 플로우]
     * 1. cursor가 null이면 처음부터, 있으면 해당 포스트 이후부터 조회
     * 2. limit+1 개 조회 → hasNext 판단 → limit개 반환
     *
     * 입력: userId, cursor (없으면 null), limit
     * 호출: PostRepository.findByUserIdOrderByCreatedAtDesc 또는 findByUserIdAfterCursor
     * 반환: CursorResponse(data[], nextCursor, hasNext)
     */
    @Override
    public CursorResponse<PostResponse> getMyPosts(UUID userId, UUID cursor, int limit) {
        Slice<Post> slice = cursor == null
            ? postRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit + 1))
            : postRepository.findByUserIdAfterCursor(userId, cursor, PageRequest.of(0, limit + 1));
        return buildCursor(slice, limit);
    }

    /**
     * [타인 게시물 목록 조회 플로우]
     * 1. cursor가 null이면 처음부터, 있으면 해당 포스트 이후부터 조회
     * 2. limit+1 개 조회 → hasNext 판단 → limit개 반환
     * getMyPosts와 동일한 쿼리를 재사용하며, 대상 유저 ID만 다르다.
     *
     * 입력: targetUserId (조회할 유저), cursor (없으면 null), limit
     * 호출: PostRepository.findByUserIdOrderByCreatedAtDesc 또는 findByUserIdAfterCursor
     * 반환: CursorResponse(data[], nextCursor, hasNext)
     */
    @Override
    public CursorResponse<PostResponse> getUserPosts(UUID targetUserId, UUID cursor, int limit) {
        Slice<Post> slice = cursor == null
            ? postRepository.findByUserIdOrderByCreatedAtDesc(targetUserId, PageRequest.of(0, limit + 1))
            : postRepository.findByUserIdAfterCursor(targetUserId, cursor, PageRequest.of(0, limit + 1));
        return buildCursor(slice, limit);
    }

    /**
     * [팀 피드 조회 플로우]
     * 전달된 유저 ID 목록의 포스트를 최신순 커서 페이지네이션으로 반환한다.
     * TeamServiceImpl이 팀 멤버 ID 목록을 조회한 뒤 이 메서드를 호출한다.
     *
     * 입력: memberIds (팀 멤버 UUID 목록), cursor (null이면 첫 페이지), limit
     * 호출: PostRepository.findFeed 또는 findFeedAfterCursor
     * 반환: CursorResponse(data, nextCursor, hasNext)
     */
    @Override
    public CursorResponse<PostResponse> getFeedForMembers(List<UUID> memberIds, UUID cursor, int limit) {
        if (memberIds.isEmpty()) {
            return new CursorResponse<>(List.of(), null, false);
        }
        Slice<Post> slice = cursor == null
            ? postRepository.findFeed(memberIds, PageRequest.of(0, limit + 1))
            : postRepository.findFeedAfterCursor(memberIds, cursor, PageRequest.of(0, limit + 1));
        return buildCursor(slice, limit);
    }

    /**
     * [게시물 수정 플로우]
     * 1. postId로 Post 조회 (없으면 POST_NOT_FOUND)
     * 2. 소유자 검증 (userId != post.userId → POST_FORBIDDEN)
     * 3. content, imageUrls 필드 업데이트 (null이면 기존값 유지)
     * 4. tagNames가 있으면 Kafka로 태그 재처리 위임
     *
     * 입력: postId, userId, req (content/imageUrls/tagNames)
     * 반환: 수정된 PostResponse
     */
    @Override
    @Transactional
    public PostResponse updatePost(UUID postId, UUID userId, UpdatePostRequest req) {
        Post post = findPost(postId);
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_FORBIDDEN);
        }

        String[] images = req.getImageUrls() != null
            ? req.getImageUrls().toArray(new String[0])
            : null;
        post.update(req.getContent(), images);

        // 태그 변경 요청이 있으면 Kafka로 재처리 위임
        if (req.getTagNames() != null && !req.getTagNames().isEmpty()) {
            postEventProducer.ifPresent(p ->
                p.publishPostCreated(new PostCreatedEvent(post.getId(), userId, req.getTagNames()))
            );
        }

        User author = userRepository.findById(userId).orElse(null);
        List<String> tags = loadTagNames(List.of(post.getId())).getOrDefault(post.getId(), List.of());
        return PostResponse.from(post, author, tags, null);
    }

    /**
     * [팀 오너 포스트 강제 삭제]
     * 소유자 여부와 관계없이 포스트를 삭제한다.
     * 팀 오너 권한 검증은 호출자(TeamServiceImpl)가 담당한다.
     *
     * 입력: postId
     * 호출: PostRepository.findById → PostRepository.delete
     * 반환: void
     */
    @Override
    @Transactional
    public void forceDeletePost(UUID postId) {
        Post post = findPost(postId);
        postRepository.delete(post);
    }

    /**
     * [포스트 조회 공통 메서드]
     * posts 테이블에서 id로 조회 (없으면 POST_NOT_FOUND).
     *
     * 입력: id (UUID)
     * 호출: PostRepository.findById
     * 반환: Post 엔티티
     */
    private Post findPost(UUID id) {
        return postRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * [커서 페이지네이션 응답 구성 — 피드/탐색 공통]
     *
     * N+1 방지 전략:
     *   - author:  유저 ID 목록을 한 번에 IN 조회 → userMap (userId → User)
     *   - tags:    포스트 ID 목록으로 post_tags 한 번 조회 → tagMap (postId → 태그명 목록)
     *   - todo:    todoId가 있는 포스트만 추려 고유 todoId별로 한 번씩 조회 → todoMap
     *             (동일 투두를 여러 포스트가 참조해도 1회만 조회)
     *
     * 커서 방식:
     *   limit+1 개 조회 → 마지막 1개 존재 여부로 hasNext 판단 →
     *   실제 반환은 limit 개, nextCursor = 마지막 포스트 UUID
     */
    private CursorResponse<PostResponse> buildCursor(Slice<Post> slice, int limit) {
        List<Post> content = slice.getContent();
        boolean hasNext = content.size() > limit;
        List<Post> data = hasNext ? content.subList(0, limit) : content;

        // author 배치 로딩 (N+1 방지)
        List<UUID> userIds = data.stream().map(Post::getUserId).distinct().toList();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 태그 배치 로딩 (N+1 방지)
        List<UUID> postIds = data.stream().map(Post::getId).toList();
        Map<UUID, List<String>> tagMap = loadTagNames(postIds);

        // 투두 요약 로딩 — todoId가 있는 포스트만, 동일 todoId 중복 로딩 없이
        Map<UUID, PostResponse.TodoSummary> todoMap = new HashMap<>();
        for (Post post : data) {
            UUID todoId = post.getTodoId();
            if (todoId != null && !todoMap.containsKey(todoId)) {
                PostResponse.TodoSummary summary = loadTodoSummary(todoId);
                if (summary != null) todoMap.put(todoId, summary);
            }
        }

        UUID nextCursor = hasNext ? data.get(data.size() - 1).getId() : null;
        return new CursorResponse<>(
            data.stream().map(p -> PostResponse.from(
                p,
                userMap.get(p.getUserId()),
                tagMap.getOrDefault(p.getId(), List.of()),
                todoMap.get(p.getTodoId())
            )).toList(),
            nextCursor,
            hasNext
        );
    }

    private Map<UUID, List<String>> loadTagNames(List<UUID> postIds) {
        List<PostTag> postTags = postTagRepository.findByPostIdIn(postIds);
        if (postTags.isEmpty()) return Map.of();

        List<UUID> tagIds = postTags.stream().map(PostTag::getTagId).distinct().toList();
        Map<UUID, String> tagNameMap = tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));

        return postTags.stream().collect(Collectors.groupingBy(
                PostTag::getPostId,
                Collectors.mapping(pt -> tagNameMap.getOrDefault(pt.getTagId(), ""), Collectors.toList())
        ));
    }

    /**
     * [TodoSummary 변환]
     *
     * 도메인 간 참조는 TodoService 인터페이스를 통해서만 수행 (직접 repository 주입 금지).
     * 투두 삭제 후 남은 포스트처럼 참조가 깨진 경우 null을 반환해 포스트 조회를 깨트리지 않는다.
     *
     * Todo.items는 @OneToMany LAZY → 호출 시 추가 쿼리 발생.
     * 이 메서드가 @Transactional 컨텍스트 안에서 호출되므로 세션이 열려 있어 정상 로딩됨.
     */
    private PostResponse.TodoSummary loadTodoSummary(UUID todoId) {
        if (todoId == null) return null;
        try {
            Todo todo = todoService.findTodo(todoId);
            List<PostResponse.TodoSummary.ItemInfo> items = todo.getItems().stream()
                    .map(item -> PostResponse.TodoSummary.ItemInfo.builder()
                            .id(item.getId())
                            .content(item.getContent())
                            .completed(item.isDone())
                            .build())
                    .toList();
            return PostResponse.TodoSummary.builder()
                    .id(todo.getId())
                    .title(todo.getTitle())
                    .items(items)
                    .build();
        } catch (BusinessException e) {
            return null;
        }
    }
}
