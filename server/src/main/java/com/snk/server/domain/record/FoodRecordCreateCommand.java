package com.snk.server.domain.record;

import java.time.OffsetDateTime;
import java.util.List;

public record FoodRecordCreateCommand(
	Long userId,
	Long foodItemId,
	String sourceType,
	boolean isPublic,
	short rating,
	String comment,
	OffsetDateTime recordTime,
	List<FoodRecordImageValue> images
) {
}
