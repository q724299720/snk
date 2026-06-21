package com.snk.server.domain.record;

public record FoodRecordUpdateCommand(
	Long recordId,
	Long userId,
	short rating,
	String comment,
	boolean isPublic
) {
}
