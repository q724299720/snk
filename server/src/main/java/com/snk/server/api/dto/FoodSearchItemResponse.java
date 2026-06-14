package com.snk.server.api.dto;

import com.snk.server.domain.food.FoodSearchItem;

public record FoodSearchItemResponse(
	Long id,
	String name,
	String itemType,
	String category,
	String subcategory,
	String brand,
	String barcode,
	String coverImageUrl,
	String auditStatus
) {

	public static FoodSearchItemResponse from(FoodSearchItem item) {
		return new FoodSearchItemResponse(
			item.id(),
			item.name(),
			item.itemType(),
			item.category(),
			item.subcategory(),
			item.brand(),
			item.barcode(),
			item.coverImageUrl(),
			item.auditStatus()
		);
	}
}
