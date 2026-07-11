ALTER TABLE users
    ADD COLUMN username VARCHAR(64),
    ADD COLUMN password_hash VARCHAR(255),
    ADD COLUMN role VARCHAR(16),
    ADD COLUMN account_status VARCHAR(16),
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN approved_by_user_id BIGINT,
    ADD COLUMN approved_at TIMESTAMPTZ,
    ADD COLUMN last_login_at TIMESTAMPTZ,
    ADD COLUMN claimed_by_user_id BIGINT,
    ADD COLUMN claimed_at TIMESTAMPTZ;

ALTER TABLE users
    DROP CONSTRAINT ck_users_auth_provider;

ALTER TABLE users
    ADD CONSTRAINT ck_users_auth_provider
        CHECK (auth_provider IN ('anonymous', 'password', 'phone', 'email', 'wechat', 'apple', 'google')),
    ADD CONSTRAINT ck_users_account_role
        CHECK (role IS NULL OR role IN ('OWNER', 'USER')),
    ADD CONSTRAINT ck_users_account_status
        CHECK (account_status IS NULL OR account_status IN ('PENDING', 'ACTIVE', 'REJECTED', 'DISABLED')),
    ADD CONSTRAINT ck_users_token_version
        CHECK (token_version >= 0),
    ADD CONSTRAINT ck_users_password_account_fields
        CHECK (
            (
                auth_provider = 'password'
                AND username IS NOT NULL
                AND password_hash IS NOT NULL
                AND role IS NOT NULL
                AND account_status IS NOT NULL
            )
            OR (
                auth_provider <> 'password'
                AND username IS NULL
                AND password_hash IS NULL
                AND role IS NULL
                AND account_status IS NULL
            )
        ),
    ADD CONSTRAINT fk_users_approved_by_user
        FOREIGN KEY (approved_by_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_users_claimed_by_user
        FOREIGN KEY (claimed_by_user_id)
        REFERENCES users (id),
    ADD CONSTRAINT ck_users_claim_pair
        CHECK (
            (claimed_by_user_id IS NULL AND claimed_at IS NULL)
            OR
            (claimed_by_user_id IS NOT NULL AND claimed_at IS NOT NULL AND claimed_by_user_id <> id)
        );

CREATE UNIQUE INDEX uk_users_username_ci
    ON users (LOWER(username))
    WHERE username IS NOT NULL;

CREATE INDEX idx_users_approved_by_user_id
    ON users (approved_by_user_id)
    WHERE approved_by_user_id IS NOT NULL;

CREATE INDEX idx_users_active_owner
    ON users (id)
    WHERE role = 'OWNER' AND account_status = 'ACTIVE';

CREATE INDEX idx_users_claimed_by_user_id
    ON users (claimed_by_user_id)
    WHERE claimed_by_user_id IS NOT NULL;
