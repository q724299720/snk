ALTER TABLE food_records
    ADD COLUMN like_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE food_records
    ADD CONSTRAINT ck_food_records_like_count
        CHECK (like_count >= 0);
