package com.snk.server.domain.food;

public record UpdateFoodItemCommand(
	String name,
	String itemType,
	String category,
	String subcategory,
	String brand,
	String alias,
	String searchKeywords
) {
}
