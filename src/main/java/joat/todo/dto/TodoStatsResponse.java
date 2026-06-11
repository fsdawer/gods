package joat.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투두 완료율·스트릭 통계 응답 DTO.
 * GET /api/todos/stats 응답에 사용된다.
 * 앱의 갓생 피드백 화면(완료율 게이지, 연속 달성일 뱃지)에 필요한 수치를 제공한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoStatsResponse {

    /** 유저가 등록한 전체 투두 수 */
    private int totalTodos;

    /** 모든 항목이 완료(isDone=true)된 투두 수 (항목이 0개인 투두는 제외) */
    private int completedTodos;

    /** 완료율 = completedTodos / totalTodos * 100, 소수점 1자리 반올림 (투두가 없으면 0.0) */
    private double completionRate;

    /** 오늘 기준으로 연속으로 "완료된 날" 수 (오늘 포함, 연속이 끊기면 중단) */
    private int currentStreak;

    /** 역대 최장 연속 달성일 */
    private int longestStreak;
}
