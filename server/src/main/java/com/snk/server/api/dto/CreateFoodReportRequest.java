package com.snk.server.api.dto;

import jakarta.validation.constraints.NotNull;

public record CreateFoodReportRequest(
	@NotNull(message = "userId is required")
	Long userId,
	String reason
) {
}
