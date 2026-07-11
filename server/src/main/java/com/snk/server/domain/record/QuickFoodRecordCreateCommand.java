package com.snk.server.domain.record;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record QuickFoodRecordCreateCommand(
	UUID clientRequestId,
	Long userId,
	String name,
	boolean isPublic,
	short rating,
	String comment,
	OffsetDateTime recordTime,
	List<FoodRecordImageValue> images
) {
}
