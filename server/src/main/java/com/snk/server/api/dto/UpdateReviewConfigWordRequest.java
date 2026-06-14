package com.snk.server.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReviewConfigWordRequest(
	@NotBlank
	@Size(max = 128)
	String word,
	@NotBlank
	@Size(max = 64)
	String wordType,
	@NotBlank
	@Size(max = 32)
	String source,
	@Size(max = 2000)
	String remark,
	@NotBlank
	@Size(max = 64)
	String operatorId,
	@NotBlank
	@Size(max = 128)
	String operatorName
) {
}
