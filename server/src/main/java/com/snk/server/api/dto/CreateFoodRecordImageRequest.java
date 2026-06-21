package com.snk.server.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFoodRecordImageRequest(
	@NotBlank(message = "imageUrl is required")
	String imageUrl,
	String thumbnailUrl
) {
}
