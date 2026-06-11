package joat.todo.controller;

import jakarta.validation.Valid;
import joat.common.response.ApiResponse;
import joat.feed.dto.CreatePostRequest;
import joat.feed.dto.PostResponse;
import joat.feed.service.PostService;
import joat.todo.dto.CertifyRequest;
import joat.todo.dto.CreateTodoRequest;
import joat.todo.dto.TodoResponse;
import joat.todo.dto.TodoStatsResponse;
import joat.todo.dto.UpdateTodoItemRequest;
import joat.todo.dto.UpdateTodoRequest;
import joat.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 투두리스트 관련 API 컨트롤러.
 * 투두 CRUD, 항목 체크, 타인 공개 투두 조회, 투두 인증 포스트 작성을 제공한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    private final PostService postService;

    /**
     * [엔드포인트] POST /api/todos — 투두리스트 생성
     * 제목, 공개여부, 날짜, 초기 항목 목록을 받아 투두를 생성한다.
     */
    @PostMapping
    public ApiResponse<TodoResponse> create(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody CreateTodoRequest req
    ) {
        return ApiResponse.ok(todoService.createTodo(userId, req));
    }

    /**
     * [엔드포인트] GET /api/todos — 내 투두 목록 조회
     * date 파라미터가 없으면 오늘 날짜로 조회한다.
     */
    @GetMapping
    public ApiResponse<List<TodoResponse>> getMine(
        @AuthenticationPrincipal UUID userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(todoService.getMyTodos(userId, date != null ? date : LocalDate.now()));
    }

    /**
     * [엔드포인트] PATCH /api/todos/{todoId} — 투두 수정
     * 제목·공개여부·항목 목록을 변경할 수 있다. items를 보내면 기존 항목 전체가 교체된다.
     */
    @PatchMapping("/{todoId}")
    public ApiResponse<TodoResponse> update(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID todoId,
        @RequestBody UpdateTodoRequest req
    ) {
        return ApiResponse.ok(todoService.updateTodo(todoId, userId, req));
    }

    /**
     * [엔드포인트] DELETE /api/todos/{todoId} — 투두 삭제
     * 본인 투두가 아니면 TODO_ACCESS_DENIED 예외가 발생한다.
     */
    @DeleteMapping("/{todoId}")
    public ApiResponse<Void> delete(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID todoId
    ) {
        todoService.deleteTodo(todoId, userId);
        return ApiResponse.ok();
    }

    /**
     * [엔드포인트] PATCH /api/todos/{todoId}/items/{itemId} — 항목 체크/해제
     * completed=true이면 완료, false이면 미완료로 변경한다.
     */
    @PatchMapping("/{todoId}/items/{itemId}")
    public ApiResponse<TodoResponse> checkItem(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID todoId,
        @PathVariable UUID itemId,
        @RequestBody UpdateTodoItemRequest req
    ) {
        return ApiResponse.ok(todoService.checkItem(todoId, itemId, userId, req.isCompleted()));
    }

    /**
     * [엔드포인트] GET /api/todos/stats — 투두 완료율·스트릭 통계 조회
     * 유저의 전체 투두를 기반으로 완료율, 현재 연속 달성일, 역대 최장 연속 달성일을 반환한다.
     */
    @GetMapping("/stats")
    public ApiResponse<TodoStatsResponse> stats(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(todoService.getStats(userId));
    }

    /**
     * [엔드포인트] GET /api/todos/users/{userId} — 타인의 공개 투두 목록 조회
     * isPublic=true인 투두만 반환한다. 인증 없이 접근 가능하다.
     */
    @GetMapping("/users/{userId}")
    public ApiResponse<List<TodoResponse>> getPublic(@PathVariable UUID userId) {
        return ApiResponse.ok(todoService.getPublicTodos(userId));
    }

    /**
     * [엔드포인트] POST /api/todos/{todoId}/certify — 투두 인증 포스트 작성
     * 소유자 검증 후 CreatePostRequest(type=todo_cert)로 변환하여 PostService.createPost를 호출한다.
     * content, imageUrls, tagNames를 그대로 전달한다.
     */
    @PostMapping("/{todoId}/certify")
    public ApiResponse<PostResponse> certify(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID todoId,
        @Valid @RequestBody CertifyRequest req
    ) {
        log.info("[certify] todoId={}, imageUrls={}, tagNames={}", todoId, req.getImageUrls(), req.getTagNames());
        // 본인 투두인지 검증 (TODO_ACCESS_DENIED)
        todoService.findTodo(todoId).validateOwner(userId);
        CreatePostRequest postReq = new CreatePostRequest(
            req.getContent(),
            req.getImageUrls(),
            req.getTagNames(),
            todoId,
            null    // 공개 게시물 (팀방 게시물 아님)
        );
        return ApiResponse.ok(postService.createPost(userId, postReq));
    }
}
