package com.snk.server.api.dto;

import com.snk.server.domain.record.FoodRecordHistoryItem;
import java.time.OffsetDateTime;
import java.util.List;

public record FoodRecordHistoryResponse(
	Long id,
	Long userId,
	Long foodItemId,
	String foodName,
	String foodItemType,
	String foodCategory,
	String foodSubcategory,
	String foodBrand,
	String foodCoverImageUrl,
	String sourceType,
	boolean isPublic,
	short rating,
	String comment,
	int likeCount,
	OffsetDateTime recordTime,
	OffsetDateTime createdAt,
	List<FoodRecordImageResponse> images
) {

	public static FoodRecordHistoryResponse from(FoodRecordHistoryItem item) {
		return new FoodRecordHistoryResponse(
			item.id(),
			item.userId(),
			item.foodItemId(),
			item.foodName(),
			item.foodItemType(),
			item.foodCategory(),
			item.foodSubcategory(),
			item.foodBrand(),
			item.foodCoverImageUrl(),
			item.sourceType(),
			item.isPublic(),
			item.rating(),
			item.comment(),
			item.likeCount(),
			item.recordTime(),
			item.createdAt(),
			item.images().stream()
				.map(FoodRecordImageResponse::from)
				.toList()
		);
	}
}
