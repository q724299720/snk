ALTER TABLE food_items
    ADD COLUMN created_by_user_id BIGINT;

ALTER TABLE food_items
    ADD CONSTRAINT fk_food_items_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (id);

CREATE INDEX idx_food_items_created_by_user_audit_status
    ON food_items (created_by_user_id, audit_status, created_at DESC);
