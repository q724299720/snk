package com.snk.server.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateFoodReportRequest(
	@NotNull(message = "userId is required")
	@Positive(message = "userId must be positive")
	Long userId,
	String reason
) {
}
