package com.snk.server.api.dto;

import com.snk.server.domain.record.FoodRecordImageValue;

public record FoodRecordImageResponse(
	String imageUrl,
	String thumbnailUrl
) {

	public static FoodRecordImageResponse from(FoodRecordImageValue image) {
		return new FoodRecordImageResponse(image.imageUrl(), image.thumbnailUrl());
	}
}
