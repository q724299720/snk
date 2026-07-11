package com.snk.server.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFoodItemRequest(
	@NotBlank @Size(max = 255) String name,
	@NotBlank String itemType,
	@NotBlank @Size(max = 64) String category,
	@Size(max = 64) String subcategory,
	@Size(max = 128) String brand,
	String alias,
	String searchKeywords
) {
}
