package joat.todo.service;

import joat.todo.entity.Todo;
import joat.todo.dto.CreateTodoRequest;
import joat.todo.dto.TodoResponse;
import joat.todo.dto.UpdateTodoRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 투두 서비스 인터페이스.
 * 투두리스트 생성/조회/삭제, 항목 체크, 공개 투두 조회를 담당한다.
 */
public interface TodoService {

    /**
     * 투두리스트 생성.
     *
     * @param userId 생성자 UUID
     * @param req    제목, 공개여부, 날짜, 항목 목록
     * @return 생성된 TodoResponse
     */
    TodoResponse createTodo(UUID userId, CreateTodoRequest req);

    /**
     * 내 특정 날짜의 투두 목록 조회.
     *
     * @param userId 조회자 UUID
     * @param date   조회 날짜
     * @return TodoResponse 목록
     */
    List<TodoResponse> getMyTodos(UUID userId, LocalDate date);

    /**
     * 타인의 공개 투두 목록 조회.
     *
     * @param userId 조회 대상 유저 UUID
     * @return 공개(isPublic=true)인 TodoResponse 목록
     */
    List<TodoResponse> getPublicTodos(UUID userId);

    /**
     * 투두리스트 수정 (제목·공개여부·항목 교체).
     *
     * @param todoId      수정할 투두 UUID
     * @param userId 요청자 UUID (소유자 검증용)
     * @param req         변경할 제목·공개여부·항목 목록
     * @return 수정된 TodoResponse
     */
    TodoResponse updateTodo(UUID todoId, UUID userId, UpdateTodoRequest req);

    /**
     * 투두리스트 삭제.
     *
     * @param todoId      삭제할 투두 UUID
     * @param userId 요청자 UUID (소유자 검증용)
     */
    void deleteTodo(UUID todoId, UUID userId);

    /**
     * 투두 항목 체크/해제.
     *
     * @param todoId      대상 투두 UUID
     * @param itemId      체크할 항목 UUID
     * @param userId 요청자 UUID (소유자 검증용)
     * @param isDone      true=완료, false=미완료
     * @return 업데이트된 TodoResponse
     */
    TodoResponse checkItem(UUID todoId, UUID itemId, UUID userId, boolean isDone);

    /**
     * 투두 단건 조회 (내부 및 certify에서 사용).
     *
     * @param todoId 조회할 투두 UUID
     * @return Todo 엔티티
     */
    Todo findTodo(UUID todoId);
}
