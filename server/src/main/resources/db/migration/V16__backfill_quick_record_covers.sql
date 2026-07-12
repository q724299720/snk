UPDATE food_items fi
SET cover_image_url = first_images.image_url
FROM (
    SELECT DISTINCT ON (fr.food_item_id)
        fr.food_item_id,
        fri.image_url
    FROM food_records fr
    JOIN food_record_images fri ON fri.record_id = fr.id
    WHERE fr.deleted_at IS NULL
      AND fr.is_public = TRUE
    ORDER BY fr.food_item_id, fr.record_time DESC, fri.created_at ASC
) first_images
WHERE fi.id = first_images.food_item_id
  AND fi.cover_image_url IS NULL;
