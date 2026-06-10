package joat.todo.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.todo.entity.Todo;
import joat.todo.repository.TodoItemRepository;
import joat.todo.repository.TodoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @InjectMocks TodoServiceImpl todoService;
    @Mock TodoRepository todoRepository;
    @Mock TodoItemRepository todoItemRepository;

    @Test
    void 다른_유저의_투두를_삭제하면_TODO_ACCESS_DENIED_예외가_발생한다() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Todo todo = Todo.of(owner, "갓생 투두", false, LocalDate.now());
        given(todoRepository.findById(todo.getId())).willReturn(Optional.of(todo));

        assertThatThrownBy(() -> todoService.deleteTodo(todo.getId(), other))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.TODO_ACCESS_DENIED);
    }

    @Test
    void 없는_투두_조회시_TODO_NOT_FOUND_예외가_발생한다() {
        UUID todoId = UUID.randomUUID();
        given(todoRepository.findById(todoId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.findTodo(todoId))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.TODO_NOT_FOUND);
    }
}
