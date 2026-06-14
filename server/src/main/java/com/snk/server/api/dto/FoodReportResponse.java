package com.snk.server.api.dto;

public record FoodReportResponse(
	Long foodItemId,
	Integer reportCount,
	String auditStatus
) {
}
