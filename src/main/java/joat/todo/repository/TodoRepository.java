package joat.todo.repository;

import joat.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TodoRepository extends JpaRepository<Todo, UUID> {
    // 특정 유저의 특정 날짜 투두 목록 조회 (내 투두 날짜별 조회)
    List<Todo> findByUserIdAndDate(UUID userId, LocalDate date);
    // 특정 유저의 공개 투두 목록 조회 (타인 프로필 페이지에서 공개 루틴 조회)
    List<Todo> findByUserIdAndIsPublicTrue(UUID userId);
}
