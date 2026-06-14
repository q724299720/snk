package com.snk.server.api.dto;

import java.time.OffsetDateTime;

public record FoodRecordResponse(
	Long id,
	Long userId,
	Long foodItemId,
	String sourceType,
	boolean isPublic,
	short rating,
	String comment,
	int likeCount,
	OffsetDateTime recordTime,
	OffsetDateTime createdAt
) {
}
