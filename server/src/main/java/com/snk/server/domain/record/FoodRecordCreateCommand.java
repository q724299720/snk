package com.snk.server.domain.record;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FoodRecordCreateCommand(
	Long userId,
	Long foodItemId,
	String sourceType,
	boolean isPublic,
	short rating,
	String comment,
	OffsetDateTime recordTime,
	List<FoodRecordImageValue> images,
	UUID clientRequestId
) {
	public FoodRecordCreateCommand(
		Long userId,
		Long foodItemId,
		String sourceType,
		boolean isPublic,
		short rating,
		String comment,
		OffsetDateTime recordTime,
		List<FoodRecordImageValue> images
	) {
		this(userId, foodItemId, sourceType, isPublic, rating, comment, recordTime, images, null);
	}
}
