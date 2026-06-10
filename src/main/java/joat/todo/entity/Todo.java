package joat.todo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import joat.common.entity.BaseEntity;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "todos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // UUID v7 기반 기본키

    @Column(nullable = false)
    private UUID userId; // 투두 소유자 UUID

    @Column(nullable = false, length = 100)
    private String title; // 투두리스트 제목 (최대 100자)

    @Column(nullable = false)
    private boolean isPublic; // 공개 여부 (true=공개, false=비공개)

    @Column(nullable = false)
    private LocalDate date; // 투두 날짜 (특정 날짜의 할 일 목록)

    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIdx ASC")
    private List<TodoItem> items = new ArrayList<>(); // 투두 항목 목록 (orderIdx 오름차순 정렬)

    public static Todo of(UUID userId, String title, boolean isPublic, LocalDate date) {
        Todo todo = new Todo();
        todo.userId = userId;
        todo.title = title;
        todo.isPublic = isPublic;
        todo.date = date;
        return todo;
    }

    public void update(String title, Boolean isPublic) {
        if (title != null) this.title = title;
        if (isPublic != null) this.isPublic = isPublic;
    }

    public void validateOwner(UUID userId) {
        if (!this.userId.equals(userId)) {
            throw new BusinessException(ErrorCode.TODO_ACCESS_DENIED);
        }
    }
}
