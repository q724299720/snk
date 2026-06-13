package com.snk.server.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateFoodRecordRequest(
	@NotNull(message = "userId is required")
	Long userId,
	@NotNull(message = "foodItemId is required")
	Long foodItemId,
	@NotBlank(message = "sourceType is required")
	String sourceType,
	boolean isPublic,
	@Min(value = 1, message = "rating must be between 1 and 5")
	@Max(value = 5, message = "rating must be between 1 and 5")
	short rating,
	String comment,
	OffsetDateTime recordTime
) {
}
