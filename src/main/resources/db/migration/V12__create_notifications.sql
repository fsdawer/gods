CREATE TABLE notifications (
    id          UUID        PRIMARY KEY,
    receiver_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,          -- LIKE | COMMENT | FOLLOW
    target_id   UUID,                          -- postId (LIKE·COMMENT) or null (FOLLOW)
    message     TEXT        NOT NULL,
    is_read     BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_receiver ON notifications (receiver_id, created_at DESC);
