package com.snk.server.api.dto;

import com.snk.server.domain.food.FoodModerationAutoAuditService.AutoAuditSummary;
import java.time.OffsetDateTime;
import java.util.List;

public record AutoAuditSummaryResponse(
	int scannedCount,
	int rejectedCount,
	int keptPendingCount,
	OffsetDateTime cutoffAt,
	List<Long> rejectedFoodItemIds
) {

	public static AutoAuditSummaryResponse from(AutoAuditSummary summary) {
		return new AutoAuditSummaryResponse(
			summary.scannedCount(),
			summary.rejectedCount(),
			summary.keptPendingCount(),
			summary.cutoffAt(),
			summary.rejectedFoodItemIds()
		);
	}
}
