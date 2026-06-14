package com.snk.server.domain.review;

public record CreateReviewConfigWordCommand(
	String word,
	String wordType,
	String source,
	String remark,
	Boolean enabled,
	String operatorId,
	String operatorName
) {
}
