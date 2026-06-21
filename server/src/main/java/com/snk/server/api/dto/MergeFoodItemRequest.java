package com.snk.server.api.dto;

import jakarta.validation.constraints.NotNull;

public record MergeFoodItemRequest(
	@NotNull
	Long targetFoodItemId
) {
}
