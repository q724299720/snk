package com.snk.server.domain.record;

import java.time.OffsetDateTime;

public record FoodRecordCreateCommand(
	Long userId,
	Long foodItemId,
	String sourceType,
	boolean isPublic,
	short rating,
	String comment,
	OffsetDateTime recordTime
) {
}
