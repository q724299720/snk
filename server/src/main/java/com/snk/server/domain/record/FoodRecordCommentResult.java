package com.snk.server.domain.record;

import java.time.OffsetDateTime;

public record FoodRecordCommentResult(
	Long id,
	Long recordId,
	Long userId,
	String content,
	OffsetDateTime createdAt
) {
}
