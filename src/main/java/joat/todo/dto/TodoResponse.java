package joat.todo.dto;

import joat.todo.entity.Todo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 투두리스트 응답 DTO.
 * GET /api/todos, POST /api/todos, PATCH /api/todos/{todoId} 등 응답에 사용된다.
 * completedCount/totalCount를 포함하여 앱의 프로그레스바 렌더링을 지원한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoResponse {
    /** 투두 UUID */
    private UUID id;
    /** 투두 제목 */
    private String title;
    /** 공개 여부 */
    private boolean isPublic;
    /** 투두 날짜 */
    private LocalDate date;
    /** 항목 목록 (orderIdx 오름차순) */
    private List<TodoItemResponse> items;
    /** 완료된 항목 수 (프로그레스바 표시용) */
    private int completedCount;
    /** 전체 항목 수 (프로그레스바 표시용) */
    private int totalCount;

    public static TodoResponse from(Todo todo) {
        List<TodoItemResponse> items = todo.getItems().stream()
                .map(TodoItemResponse::from)
                .toList();
        return TodoResponse.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .isPublic(todo.isPublic())
                .date(todo.getDate())
                .items(items)
                .completedCount((int) items.stream().filter(TodoItemResponse::isCompleted).count())
                .totalCount(items.size())
                .build();
    }
}
