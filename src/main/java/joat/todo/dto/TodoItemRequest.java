package joat.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투두 항목 요청 DTO.
 * CreateTodoRequest.items 및 UpdateTodoRequest.items의 원소로 사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TodoItemRequest {
    /** 항목 내용 (최대 200자) */
    private String content;
}
