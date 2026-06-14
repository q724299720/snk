package com.snk.server.api.dto;

import com.snk.server.domain.admin.AdminStatsService.AdminStatsResult;

public record AdminStatsResponse(
	long totalFoodItems,
	long pendingFoodItems,
	long approvedFoodItems,
	long rejectedFoodItems,
	long reportedFoodItems,
	long totalRecognitionTasks,
	long processingRecognitionTasks,
	long completedRecognitionTasks,
	long failedRecognitionTasks,
	long enabledReviewWords,
	long disabledReviewWords
) {

	public static AdminStatsResponse from(AdminStatsResult result) {
		return new AdminStatsResponse(
			result.totalFoodItems(),
			result.pendingFoodItems(),
			result.approvedFoodItems(),
			result.rejectedFoodItems(),
			result.reportedFoodItems(),
			result.totalRecognitionTasks(),
			result.processingRecognitionTasks(),
			result.completedRecognitionTasks(),
			result.failedRecognitionTasks(),
			result.enabledReviewWords(),
			result.disabledReviewWords()
		);
	}
}
