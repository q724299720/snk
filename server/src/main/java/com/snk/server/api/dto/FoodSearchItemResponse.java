package com.snk.server.api.dto;

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
}
