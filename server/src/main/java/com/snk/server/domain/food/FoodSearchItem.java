package com.snk.server.domain.food;

import java.math.BigDecimal;

public record FoodSearchItem(
	Long id,
	String name,
	String itemType,
	String category,
	String subcategory,
	String brand,
	String barcode,
	String coverImageUrl,
	BigDecimal averageRating,
	String auditStatus
) {
}
