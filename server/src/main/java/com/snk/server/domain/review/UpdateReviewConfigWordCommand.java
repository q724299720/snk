package com.snk.server.domain.review;

public record UpdateReviewConfigWordCommand(
	Long id,
	String word,
	String wordType,
	String source,
	String remark,
	String operatorId,
	String operatorName
) {
}
