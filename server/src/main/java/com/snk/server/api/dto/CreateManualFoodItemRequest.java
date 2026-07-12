package com.snk.server.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateManualFoodItemRequest(
	@NotBlank(message = "name is required")
	String name,
	@NotBlank(message = "itemType is required")
	String itemType,
	String brand,
	String barcode,
	String coverImageUrl
) {
}
