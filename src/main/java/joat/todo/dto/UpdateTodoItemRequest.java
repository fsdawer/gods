package joat.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투두 항목 체크/해제 요청 DTO.
 * PATCH /api/todos/{todoId}/items/{itemId} 에서 사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTodoItemRequest {
    private boolean completed; // 프론트엔드 필드명과 일치 (true=완료, false=미완료)
}
