package joat.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 투두리스트 생성 요청 DTO.
 * POST /api/todos 에서 사용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTodoRequest {
    @NotBlank
    /** 투두리스트 제목 (필수) */
    private String title;
    /** 공개 여부 (true=다른 유저에게 공개, false=비공개) */
    private boolean isPublic;
    @NotNull
    /** 투두 날짜 (필수, ISO-8601 형식: yyyy-MM-dd) */
    private LocalDate date;
    /** 초기 항목 목록 (없으면 null, 나중에 추가 가능) */
    private List<TodoItemRequest> items;
}
