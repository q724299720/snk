package com.snk.server.api.dto;

import com.snk.server.domain.record.FoodRecordCommentResult;
import java.time.OffsetDateTime;

public record FoodRecordCommentResponse(
	Long id,
	Long recordId,
	Long userId,
	String content,
	OffsetDateTime createdAt
) {

	public static FoodRecordCommentResponse from(FoodRecordCommentResult result) {
		return new FoodRecordCommentResponse(
			result.id(),
			result.recordId(),
			result.userId(),
			result.content(),
			result.createdAt()
		);
	}
}
