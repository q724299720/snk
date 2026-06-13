ALTER TABLE users
    ADD COLUMN anonymous_installation_id VARCHAR(128),
    ADD COLUMN last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE UNIQUE INDEX uk_users_anonymous_installation_id
    ON users (anonymous_installation_id)
    WHERE anonymous_installation_id IS NOT NULL AND auth_provider = 'anonymous';

CREATE INDEX idx_users_last_seen_at
    ON users (last_seen_at DESC);
