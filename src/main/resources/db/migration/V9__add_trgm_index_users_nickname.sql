-- pg_trgm 확장 활성화 (이미 존재하면 무시)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 닉네임 부분일치 검색(LIKE '%q%')에서 풀 테이블 스캔 방지용 GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_users_nickname_trgm ON users USING GIN (nickname gin_trgm_ops);
