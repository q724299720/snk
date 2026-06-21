package com.snk.server.domain.record;

import java.util.List;

public record FoodRecordUpdateCommand(
	Long recordId,
	Long userId,
	short rating,
	String comment,
	boolean isPublic,
	List<FoodRecordImageValue> images
) {
}
