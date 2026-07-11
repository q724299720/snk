ALTER TABLE food_items
    DROP CONSTRAINT ck_food_items_item_type;

ALTER TABLE food_items
    ADD CONSTRAINT ck_food_items_item_type
        CHECK (item_type IN ('packaged_product', 'dish', 'fruit', 'unknown'));

ALTER TABLE food_records
    ADD COLUMN client_request_id UUID;

CREATE UNIQUE INDEX uk_food_records_user_client_request
    ON food_records (user_id, client_request_id)
    WHERE client_request_id IS NOT NULL;
