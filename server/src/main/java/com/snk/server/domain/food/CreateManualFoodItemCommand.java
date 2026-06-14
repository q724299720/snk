package com.snk.server.domain.food;

public record CreateManualFoodItemCommand(
	Long userId,
	String name,
	String itemType,
	String category,
	String subcategory,
	String brand
) {
}
