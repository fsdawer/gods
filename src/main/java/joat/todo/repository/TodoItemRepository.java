package joat.todo.repository;

import joat.todo.entity.Todo;
import joat.todo.entity.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TodoItemRepository extends JpaRepository<TodoItem, UUID> {
    // 특정 투두에 속한 모든 항목 삭제 (투두 수정 시 항목 전체 교체에 사용)
    void deleteAllByTodo(Todo todo);
}
