package com.snk.server.domain.record;

import java.time.OffsetDateTime;

public record FoodRecordResult(
	Long id,
	Long userId,
	Long foodItemId,
	String sourceType,
	boolean isPublic,
	short rating,
	String comment,
	OffsetDateTime recordTime,
	OffsetDateTime createdAt
) {
}
