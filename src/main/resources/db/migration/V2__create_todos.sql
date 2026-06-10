CREATE TABLE todos (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      VARCHAR(100) NOT NULL,
    is_public  BOOLEAN NOT NULL DEFAULT false,
    date       DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE todo_items (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    todo_id    UUID NOT NULL REFERENCES todos(id) ON DELETE CASCADE,
    content    VARCHAR(200) NOT NULL,
    is_done    BOOLEAN NOT NULL DEFAULT false,
    order_idx  INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
