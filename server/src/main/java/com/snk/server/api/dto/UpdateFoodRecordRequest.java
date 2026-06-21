package com.snk.server.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateFoodRecordRequest(
	@NotNull(message = "userId is required")
	Long userId,
	@Min(value = 1, message = "rating must be between 1 and 5")
	@Max(value = 5, message = "rating must be between 1 and 5")
	short rating,
	@Size(max = 500, message = "comment must be at most 500 characters")
	String comment,
	boolean isPublic,
	List<@Valid CreateFoodRecordImageRequest> images
) {

	public List<CreateFoodRecordImageRequest> imagesOrEmpty() {
		return images == null ? List.of() : images;
	}
}
