package joat.todo.dto;

import joat.todo.entity.TodoItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 투두 항목 응답 DTO.
 * TodoResponse.items의 원소로 사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoItemResponse {
    /** 항목 UUID */
    private UUID id;
    /** 항목 내용 */
    private String content;
    private boolean completed; // 프론트엔드 필드명과 일치 (엔티티는 isDone)
    /** 표시 순서 (0부터 시작) */
    private int orderIdx;

    public static TodoItemResponse from(TodoItem todoItem) {
        return TodoItemResponse.builder()
                .id(todoItem.getId())
                .content(todoItem.getContent())
                .completed(todoItem.isDone())
                .orderIdx(todoItem.getOrderIdx())
                .build();
    }
}
