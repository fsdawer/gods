-- 포스트에 팀방 소속 여부 컬럼 추가.
-- team_id IS NULL  → 공개 게시물 (탐색·태그 검색 노출)
-- team_id IS NOT NULL → 팀방 전용 게시물 (탐색·태그 검색 제외, 팀 피드에서만 노출)
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS team_id UUID REFERENCES team_rooms(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_posts_team_id ON posts(team_id);
