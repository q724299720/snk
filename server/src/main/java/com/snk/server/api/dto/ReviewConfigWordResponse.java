package com.snk.server.api.dto;

import com.snk.server.domain.review.ReviewConfigWordService.ReviewConfigWordItem;
import java.time.OffsetDateTime;

public record ReviewConfigWordResponse(
	Long id,
	String word,
	String wordType,
	boolean enabled,
	String source,
	String remark,
	String updatedBy,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {
	public static ReviewConfigWordResponse from(ReviewConfigWordItem item) {
		return new ReviewConfigWordResponse(
			item.id(),
			item.word(),
			item.wordType(),
			item.enabled(),
			item.source(),
			item.remark(),
			item.updatedBy(),
			item.createdAt(),
			item.updatedAt()
		);
	}
}
