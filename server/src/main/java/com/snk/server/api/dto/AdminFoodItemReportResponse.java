package com.snk.server.api.dto;

import com.snk.server.domain.food.FoodFeedbackService.FoodItemReportItem;
import java.time.OffsetDateTime;

public record AdminFoodItemReportResponse(
	Long id,
	Long foodItemId,
	Long reporterUserId,
	String reason,
	OffsetDateTime createdAt
) {
	public static AdminFoodItemReportResponse from(FoodItemReportItem item) {
		return new AdminFoodItemReportResponse(
			item.id(),
			item.foodItemId(),
			item.reporterUserId(),
			item.reason(),
			item.createdAt()
		);
	}
}
