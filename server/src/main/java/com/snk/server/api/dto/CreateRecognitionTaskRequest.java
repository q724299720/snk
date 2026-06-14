package com.snk.server.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRecognitionTaskRequest(
	@NotNull
	Long userId,
	@NotBlank
	String inputImageUrl
) {
}
