package joat.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 커서 기반 페이지네이션 응답 래퍼.
 * 피드/탐색 목록 API에서 사용된다.
 * 앱은 hasNext가 true일 때 nextCursor를 다음 요청의 cursor 파라미터로 전달한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CursorResponse<T> {
    /** 현재 페이지 데이터 목록 (최대 limit개) */
    private List<T> data;
    /** 다음 페이지 조회 시 사용할 커서 (마지막 항목의 UUID, 다음 페이지 없으면 null) */
    private UUID nextCursor;
    /** 다음 페이지 존재 여부 (limit+1 개 조회 트릭으로 판단) */
    private boolean hasNext;
}
