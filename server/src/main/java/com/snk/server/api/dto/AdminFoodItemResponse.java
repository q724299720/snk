package com.snk.server.api.dto;

import com.snk.server.domain.food.FoodModerationService.FoodModerationItem;
import java.time.OffsetDateTime;

public record AdminFoodItemResponse(
	Long id,
	String name,
	String itemType,
	String category,
	String subcategory,
	String brand,
	String barcode,
	String source,
	String auditStatus,
	Integer reportCount,
	Long createdByUserId,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {
	public static AdminFoodItemResponse from(FoodModerationItem item) {
		return new AdminFoodItemResponse(
			item.id(),
			item.name(),
			item.itemType(),
			item.category(),
			item.subcategory(),
			item.brand(),
			item.barcode(),
			item.source(),
			item.auditStatus(),
			item.reportCount(),
			item.createdByUserId(),
			item.createdAt(),
			item.updatedAt()
		);
	}
}
