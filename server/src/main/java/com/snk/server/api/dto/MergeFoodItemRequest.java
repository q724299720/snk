package com.snk.server.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MergeFoodItemRequest(
	@NotNull
	@Positive
	Long targetFoodItemId
) {
}
