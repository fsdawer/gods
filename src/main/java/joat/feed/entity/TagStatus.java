package joat.feed.entity;

/** 게시물 태그 처리 상태. Kafka 컨슈머가 처리 결과에 따라 전환한다. */
public enum TagStatus {
    /** 태그 처리 대기 중 (게시물 생성 직후 초기값) */
    PENDING,
    /** Kafka 컨슈머가 정상 처리 완료 */
    DONE,
    /** DLQ 도달 — 배치 재처리 대상 */
    FAILED
}
