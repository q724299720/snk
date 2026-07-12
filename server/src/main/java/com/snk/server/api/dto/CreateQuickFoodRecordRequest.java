package com.snk.server.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateQuickFoodRecordRequest(
	@NotNull(message = "clientRequestId is required")
	UUID clientRequestId,
	@NotBlank(message = "name is required")
	@Size(max = 255, message = "name must be at most 255 characters")
	String name,
	boolean isPublic,
	@Min(value = 1, message = "rating must be between 1 and 5")
	@Max(value = 5, message = "rating must be between 1 and 5")
	int rating,
	@Size(max = 500, message = "comment must be at most 500 characters")
	String comment,
	OffsetDateTime recordTime,
	List<@Valid CreateFoodRecordImageRequest> images
) {
	public List<CreateFoodRecordImageRequest> imagesOrEmpty() {
		return images == null ? List.of() : images;
	}
}
