-- 태그 처리 상태 컬럼 추가.
-- PENDING: 태그 처리 대기 중 (초기값)
-- DONE:    Kafka 컨슈머가 정상 처리 완료
-- FAILED:  DLQ 도달 — 배치 재처리 대상
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS tag_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS pending_tag_names TEXT[];
-- pending_tag_names: DLQ 도달 시 DlqConsumer가 저장, 배치 재처리 시 이 값으로 processTags 재호출
-- tag_status = DONE 으로 전환되면 pending_tag_names 는 null 로 초기화됨
