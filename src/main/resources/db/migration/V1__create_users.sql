CREATE TYPE oauth_provider AS ENUM ('kakao', 'google');

CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nickname          VARCHAR(50) NOT NULL,
    profile_image_url TEXT,
    bio               TEXT,
    oauth_provider    oauth_provider NOT NULL,
    oauth_id          VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (oauth_provider, oauth_id)
);
