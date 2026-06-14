package com.snk.server.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateManualFoodItemRequest(
	@NotNull(message = "userId is required")
	Long userId,
	@NotBlank(message = "name is required")
	String name,
	@NotBlank(message = "itemType is required")
	String itemType,
	@NotBlank(message = "category is required")
	String category,
	String subcategory,
	String brand
) {
}
