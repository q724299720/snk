package com.snk.server.api.dto;

import com.snk.server.domain.food.FoodModerationService.FoodItemMergeResult;

public record MergeFoodItemResponse(
	AdminFoodItemResponse duplicateItem,
	AdminFoodItemResponse targetItem,
	int migratedRecordCount
) {

	public static MergeFoodItemResponse from(FoodItemMergeResult result) {
		return new MergeFoodItemResponse(
			AdminFoodItemResponse.from(result.duplicateItem()),
			AdminFoodItemResponse.from(result.targetItem()),
			result.migratedRecordCount()
		);
	}
}
