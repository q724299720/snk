package com.snk.server.domain.record;

import java.time.OffsetDateTime;
import java.util.List;

public record FoodRecordResult(
	Long id,
	Long userId,
	Long foodItemId,
	String sourceType,
	boolean isPublic,
	short rating,
	String comment,
	int likeCount,
	OffsetDateTime recordTime,
	OffsetDateTime createdAt,
	List<FoodRecordImageValue> images
) {
}
