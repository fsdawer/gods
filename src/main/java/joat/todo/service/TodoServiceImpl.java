package joat.todo.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.todo.entity.Todo;
import joat.todo.entity.TodoItem;
import joat.todo.dto.CreateTodoRequest;
import joat.todo.dto.TodoResponse;
import joat.todo.dto.TodoStatsResponse;
import joat.todo.dto.UpdateTodoRequest;
import joat.todo.repository.TodoItemRepository;
import joat.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /**
     * [통계 조회 플로우]
     * 1. JOIN FETCH로 유저의 전체 투두+항목을 한 번에 로딩 (N+1 방지)
     * 2. 항목이 1개 이상이고 모두 isDone=true인 투두를 "완료된 투두"로 집계
     * 3. 날짜별로 그룹화하여 "완료된 날(date)" 집합 생성
     *    — 해당 날짜의 모든 투두가 완료된 날만 포함 (일부만 완료된 날은 제외)
     * 4. 완료된 날 집합을 역순으로 순회하며 currentStreak·longestStreak 계산
     *
     * 입력: userId (JWT에서 파싱된 요청자 UUID)
     * 호출: TodoRepository.findAllWithItemsByUserId
     * 반환: TodoStatsResponse (totalTodos, completedTodos, completionRate, currentStreak, longestStreak)
     */
    @Override
    public TodoStatsResponse getStats(UUID userId) {
        // JOIN FETCH로 items까지 한 번에 로딩 → N+1 없이 모든 투두 처리
        List<Todo> todos = todoRepository.findAllWithItemsByUserId(userId);

        int totalTodos = todos.size();
        if (totalTodos == 0) {
            return TodoStatsResponse.builder()
                .totalTodos(0).completedTodos(0).completionRate(0.0)
                .currentStreak(0).longestStreak(0)
                .build();
        }

        // 항목이 1개 이상이고 모두 isDone=true인 투두 → "완료된 투두"
        long completedTodos = todos.stream()
            .filter(t -> !t.getItems().isEmpty() && t.getItems().stream().allMatch(TodoItem::isDone))
            .count();

        double completionRate = Math.round((double) completedTodos / totalTodos * 1000.0) / 10.0;

        // 날짜별로 투두를 묶어 "그 날짜의 모든 투두가 완료됐는지" 판단
        // → 완료된 날(date)만 TreeSet에 담아 정렬 유지 (스트릭 계산 시 연속성 체크에 사용)
        Map<LocalDate, List<Todo>> byDate = todos.stream()
            .collect(Collectors.groupingBy(Todo::getDate));

        Set<LocalDate> completedDays = new TreeSet<>();
        for (Map.Entry<LocalDate, List<Todo>> entry : byDate.entrySet()) {
            List<Todo> dayTodos = entry.getValue();
            // 해당 날짜의 모든 투두가 완료 조건을 만족해야 "완료된 날"로 인정
            boolean allComplete = dayTodos.stream()
                .allMatch(t -> !t.getItems().isEmpty() && t.getItems().stream().allMatch(TodoItem::isDone));
            if (allComplete) {
                completedDays.add(entry.getKey());
            }
        }

        int currentStreak = calcCurrentStreak(completedDays);
        int longestStreak = calcLongestStreak(completedDays);

        return TodoStatsResponse.builder()
            .totalTodos(totalTodos)
            .completedTodos((int) completedTodos)
            .completionRate(completionRate)
            .currentStreak(currentStreak)
            .longestStreak(longestStreak)
            .build();
    }

    /**
     * 오늘 기준으로 연속으로 완료된 날 수를 계산한다.
     * 오늘이 완료된 날이 아니면 0, 오늘부터 하루씩 거슬러 올라가며 끊기는 지점에서 중단.
     *
     * @param completedDays 완료된 날짜 집합 (정렬 순서 무관)
     * @return 현재 연속 달성일 수
     */
    private int calcCurrentStreak(Set<LocalDate> completedDays) {
        LocalDate cursor = LocalDate.now();
        int streak = 0;
        // 오늘부터 하루씩 이전으로 이동하면서 연속된 완료일을 카운트
        while (completedDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /**
     * 역대 최장 연속 달성일을 계산한다.
     * 완료된 날짜를 오름차순으로 순회하면서 하루씩 이어지는 구간의 최대 길이를 구한다.
     *
     * @param completedDays 완료된 날짜 집합 (TreeSet으로 오름차순 정렬 보장)
     * @return 역대 최장 연속 달성일 수
     */
    private int calcLongestStreak(Set<LocalDate> completedDays) {
        if (completedDays.isEmpty()) return 0;

        int longest = 0;
        int current = 0;
        LocalDate prev = null;

        // TreeSet이므로 오름차순 순회 보장 → 날짜 연속성을 prev와 비교
        for (LocalDate day : completedDays) {
            if (prev != null && day.equals(prev.plusDays(1))) {
                current++; // 전날과 이어지면 현재 스트릭 연장
            } else {
                current = 1; // 연속이 끊기면 새 구간 시작
            }
            if (current > longest) longest = current;
            prev = day;
        }
        return longest;
    }
}
