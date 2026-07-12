package com.snk.server.api.dto;

import com.snk.server.domain.food.FoodSearchItem;
import java.math.BigDecimal;

public record FoodSearchItemResponse(
	Long id,
	String name,
	String itemType,
	String brand,
	String barcode,
	String coverImageUrl,
	BigDecimal averageRating,
	String auditStatus
) {

	public static FoodSearchItemResponse from(FoodSearchItem item) {
		return new FoodSearchItemResponse(
			item.id(),
			item.name(),
			item.itemType(),
			item.brand(),
			item.barcode(),
			item.coverImageUrl(),
			item.averageRating(),
			item.auditStatus()
		);
	}
}
