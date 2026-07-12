package com.snk.server.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFoodRecordCommentRequest(
	@NotBlank @Size(max = 500) String content
) {
}
