-- V6: 팀방 기능 스키마
-- team_rooms, team_members, team_posts, team_messages 테이블 및 인덱스

CREATE TABLE IF NOT EXISTS team_rooms (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    created_by   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    member_count INTEGER NOT NULL DEFAULT 1,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS team_members (
    team_room_id UUID        NOT NULL REFERENCES team_rooms(id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    role         VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at    TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (team_room_id, user_id)
);

CREATE TABLE IF NOT EXISTS team_posts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_room_id UUID NOT NULL REFERENCES team_rooms(id) ON DELETE CASCADE,
    post_id      UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (team_room_id, post_id)
);

CREATE TABLE IF NOT EXISTS team_messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_room_id UUID NOT NULL REFERENCES team_rooms(id) ON DELETE CASCADE,
    sender_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content      TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_team_members_user ON team_members(user_id);
CREATE INDEX IF NOT EXISTS idx_team_messages_room_created ON team_messages(team_room_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_team_posts_room ON team_posts(team_room_id, created_at DESC);
