package joat.todo.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.todo.entity.Todo;
import joat.todo.entity.TodoItem;
import joat.todo.dto.CreateTodoRequest;
import joat.todo.dto.TodoResponse;
import joat.todo.dto.UpdateTodoRequest;
import joat.todo.repository.TodoItemRepository;
import joat.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TodoService 구현체.
 * 투두리스트 및 항목의 CRUD와 소유자 검증을 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;
    private final TodoItemRepository todoItemRepository;

    /**
     * [투두 생성 플로우]
     * 1. Todo 엔티티 생성 (userId, title, isPublic, date)
     * 2. todos 테이블에 저장
     * 3. req.items가 있으면 순서대로 todo_items 테이블에 저장 (orderIdx = 배열 인덱스)
     * 4. TodoResponse로 변환하여 반환
     *
     * 입력: userId (JWT에서 파싱), req (title, isPublic, date, items[])
     * 호출: TodoRepository.save → TodoItemRepository.save (항목 수만큼)
     * 반환: TodoResponse (id, title, isPublic, date, items[])
     */
    @Override
    @Transactional
    public TodoResponse createTodo(UUID userId, CreateTodoRequest createTodoRequest) {
        // Todo 엔티티 저장
        Todo todo = todoRepository.save(
            Todo.of(userId, createTodoRequest.getTitle(), createTodoRequest.isPublic(), createTodoRequest.getDate())
        );
        // 항목 리스트가 있으면 순서(orderIdx)를 배열 인덱스로 저장
        // saveAll로 배치 INSERT (application.yaml의 hibernate.jdbc.batch_size 설정과 함께 동작)
        if (createTodoRequest.getItems() != null) {
            List<TodoItem> items = new ArrayList<>();
            for (int i = 0; i < createTodoRequest.getItems().size(); i++) {
                items.add(TodoItem.of(todo, createTodoRequest.getItems().get(i).getContent(), i));
            }
            todoItemRepository.saveAll(items);
        }
        return TodoResponse.from(todo);
    }

    /**
     * [내 투두 목록 조회 플로우]
     * todos 테이블에서 userId AND date 조건으로 조회한다.
     *
     * 입력: userId, date (LocalDate)
     * 호출: TodoRepository.findByUserIdAndDate(userId, date)
     * 반환: TodoResponse 목록 (각 todo와 연결된 항목 포함)
     */
    @Override
    public List<TodoResponse> getMyTodos(UUID userId, LocalDate date) {
        return todoRepository.findByUserIdAndDate(userId, date)
            .stream()
            .map(TodoResponse::from)
            .toList();
    }

    /**
     * [공개 투두 목록 조회 플로우]
     * todos 테이블에서 userId AND is_public=true 조건으로 조회한다.
     * 다른 유저의 프로필 페이지에서 공개 루틴을 보여줄 때 사용.
     *
     * 입력: userId (조회 대상 유저 UUID)
     * 호출: TodoRepository.findByUserIdAndIsPublicTrue(userId)
     * 반환: 공개 TodoResponse 목록
     */
    @Override
    public List<TodoResponse> getPublicTodos(UUID userId) {
        return todoRepository.findByUserIdAndIsPublicTrue(userId)
            .stream()
            .map(TodoResponse::from)
            .toList();
    }

    /**
     * [투두 수정 플로우]
     * 1. todoId로 Todo 조회 (없으면 TODO_NOT_FOUND)
     * 2. 소유자 검증 (TODO_ACCESS_DENIED)
     * 3. 제목·공개여부 업데이트 (null이면 기존값 유지)
     * 4. items가 요청에 포함된 경우: 기존 항목 전체 삭제 후 새 항목으로 교체
     *    (완료 상태는 초기화됨 — 항목 내용이 바뀌므로)
     * 5. 업데이트된 TodoResponse 반환
     */
    @Override
    @Transactional
    public TodoResponse updateTodo(UUID todoId, UUID userId, UpdateTodoRequest req) {
        Todo todo = findTodo(todoId);
        todo.validateOwner(userId);

        todo.update(req.getTitle(), req.getIsPublic());

        if (req.getItems() != null) {
            todoItemRepository.deleteAllByTodo(todo);
            todoItemRepository.flush(); // 삭제 먼저 반영 후 insert
            for (int i = 0; i < req.getItems().size(); i++) {
                todoItemRepository.save(TodoItem.of(todo, req.getItems().get(i).getContent(), i));
            }
        }

        return TodoResponse.from(findTodo(todoId)); // flush 후 재조회로 items 최신화
    }

    /**
     * [투두 삭제 플로우]
     * 1. todoId로 Todo 엔티티 조회 (없으면 TODO_NOT_FOUND)
     * 2. 소유자 검증 — userId != userId이면 TODO_ACCESS_DENIED
     * 3. todos 테이블에서 삭제 (cascade로 todo_items도 함께 삭제)
     *
     * 입력: todoId (삭제 대상), userId (JWT에서 파싱된 요청자 UUID)
     * 호출: TodoRepository.findById → Todo.validateOwner → TodoRepository.delete
     * 반환: void
     */
    @Override
    @Transactional
    public void deleteTodo(UUID todoId, UUID userId) {
        Todo todo = findTodo(todoId);
        todo.validateOwner(userId); // 소유자가 아니면 TODO_ACCESS_DENIED 예외
        todoRepository.delete(todo);
    }

    /**
     * [항목 체크/해제 플로우]
     * 1. todoId로 Todo 조회 (없으면 TODO_NOT_FOUND)
     * 2. 소유자 검증 (TODO_ACCESS_DENIED)
     * 3. itemId로 TodoItem 조회 (없으면 TODO_ITEM_NOT_FOUND)
     * 4. isDone 값으로 is_done 필드 업데이트 (JPA dirty checking)
     * 5. 업데이트된 전체 Todo(항목 포함)를 반환
     *
     * 입력: todoId, itemId, userId, isDone (true=완료/false=미완료)
     * 호출: TodoRepository.findById → Todo.validateOwner → TodoItemRepository.findById → TodoItem.toggleDone
     * 반환: 업데이트된 TodoResponse
     */
    @Override
    @Transactional
    public TodoResponse checkItem(UUID todoId, UUID itemId, UUID userId, boolean isDone) {
        Todo todo = findTodo(todoId);
        todo.validateOwner(userId);

        // 해당 항목 조회 후 완료 상태 토글
        TodoItem todoItem = todoItemRepository.findById(itemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TODO_ITEM_NOT_FOUND));
        todoItem.toggleDone(isDone);

        return TodoResponse.from(todo);
    }

    /**
     * [투두 단건 조회 — 내부/외부 공용]
     * TodoController.certify에서도 호출됨.
     * todos 테이블에서 todoId로 조회 (없으면 TODO_NOT_FOUND).
     *
     * 입력: todoId (UUID)
     * 호출: TodoRepository.findById
     * 반환: Todo 엔티티
     */
    @Override
    public Todo findTodo(UUID todoId) {
        return todoRepository.findById(todoId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));
    }
}
