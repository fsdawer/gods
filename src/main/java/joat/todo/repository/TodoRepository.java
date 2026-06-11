package joat.todo.repository;

import joat.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TodoRepository extends JpaRepository<Todo, UUID> {
    // 특정 유저의 특정 날짜 투두 목록 조회 (내 투두 날짜별 조회)
    List<Todo> findByUserIdAndDate(UUID userId, LocalDate date);
    // 특정 유저의 공개 투두 목록 조회 (타인 프로필 페이지에서 공개 루틴 조회)
    List<Todo> findByUserIdAndIsPublicTrue(UUID userId);

    /**
     * 유저의 전체 투두와 항목을 한 번에 조회.
     * JOIN FETCH로 items를 함께 로딩해 N+1 없이 스트릭·완료율 계산에 사용한다.
     *
     * @param userId 조회 대상 유저 UUID
     * @return items가 포함된 Todo 목록
     */
    @Query("SELECT DISTINCT t FROM Todo t LEFT JOIN FETCH t.items WHERE t.userId = :userId")
    List<Todo> findAllWithItemsByUserId(@Param("userId") UUID userId);
}
