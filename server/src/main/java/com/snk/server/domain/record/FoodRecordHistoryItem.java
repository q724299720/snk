package com.snk.server.domain.record;

import java.time.OffsetDateTime;

public record FoodRecordHistoryItem(
	Long id,
	Long userId,
	Long foodItemId,
	String foodName,
	String foodItemType,
	String foodCategory,
	String foodSubcategory,
	String foodBrand,
	String foodCoverImageUrl,
	String sourceType,
	boolean isPublic,
	short rating,
	String comment,
	int likeCount,
	OffsetDateTime recordTime,
	OffsetDateTime createdAt
) {
}
