ALTER TABLE food_items ADD COLUMN is_searchable BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE food_items SET category = 'none', subcategory = NULL, audit_status = 'approved';
CREATE INDEX idx_food_items_searchable_approved ON food_items (id) WHERE audit_status = 'approved' AND is_searchable = TRUE;
