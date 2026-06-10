package joat.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 투두리스트 수정 요청 DTO.
 * PATCH /api/todos/{todoId} 에서 사용된다.
 * 변경할 필드만 포함하고, null이면 해당 필드는 기존값을 유지한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTodoRequest {
    /** 변경할 제목 (null이면 유지) */
    private String title;
    /** 변경할 공개 여부 (null이면 유지) */
    private Boolean isPublic;
    private List<TodoItemRequest> items; // null이면 항목 변경 안 함
}
