package joat.todo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import joat.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "todo_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // UUID v7 기반 기본키

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo; // 소속 투두리스트 (LAZY 로딩)

    @Column(nullable = false, length = 200)
    private String content; // 항목 내용 (최대 200자)

    @Column(nullable = false)
    private boolean isDone; // 완료 여부 (응답 DTO에서 completed로 변환)

    @Column(nullable = false)
    private int orderIdx; // 표시 순서 (0부터 시작, 오름차순 정렬)

    public static TodoItem of(Todo todo, String content, int orderIdx) {
        TodoItem todoItem = new TodoItem();
        todoItem.todo = todo;
        todoItem.content = content;
        todoItem.orderIdx = orderIdx;
        return todoItem;
    }

    public void toggleDone(boolean done) {
        this.isDone = done;
    }
}
