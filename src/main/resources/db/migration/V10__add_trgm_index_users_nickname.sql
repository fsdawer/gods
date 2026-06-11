-- pg_trgm 확장은 슈퍼유저 권한 필요 → 앱 시작 전 수동 실행 필요:
-- psql -U postgres -d joat -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"

-- 닉네임 부분일치 검색(LIKE '%q%')에서 풀 테이블 스캔 방지용 GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_users_nickname_trgm ON users USING GIN (nickname gin_trgm_ops);
