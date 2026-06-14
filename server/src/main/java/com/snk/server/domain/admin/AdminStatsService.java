package com.snk.server.domain.admin;

import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.recognition.RecognitionTaskRepository;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStatsService {

	private final FoodItemRepository foodItemRepository;
	private final RecognitionTaskRepository recognitionTaskRepository;
	private final ReviewConfigWordRepository reviewConfigWordRepository;

	public AdminStatsService(
		FoodItemRepository foodItemRepository,
		RecognitionTaskRepository recognitionTaskRepository,
		ReviewConfigWordRepository reviewConfigWordRepository
	) {
		this.foodItemRepository = foodItemRepository;
		this.recognitionTaskRepository = recognitionTaskRepository;
		this.reviewConfigWordRepository = reviewConfigWordRepository;
	}

	@Transactional(readOnly = true)
	public AdminStatsResult getStats() {
		long pendingFoodItems = foodItemRepository.countByAuditStatus("pending");
		long approvedFoodItems = foodItemRepository.countByAuditStatus("approved");
		long rejectedFoodItems = foodItemRepository.countByAuditStatus("rejected");
		long reportedFoodItems = foodItemRepository.countByReportCountGreaterThanEqual(1);
		long processingTasks = recognitionTaskRepository.countByStatus("processing");
		long completedTasks = recognitionTaskRepository.countByStatus("completed");
		long failedTasks = recognitionTaskRepository.countByStatus("failed");
		long enabledReviewWords = reviewConfigWordRepository.countByEnabled(true);
		long disabledReviewWords = reviewConfigWordRepository.countByEnabled(false);

		return new AdminStatsResult(
			pendingFoodItems + approvedFoodItems + rejectedFoodItems,
			pendingFoodItems,
			approvedFoodItems,
			rejectedFoodItems,
			reportedFoodItems,
			processingTasks + completedTasks + failedTasks,
			processingTasks,
			completedTasks,
			failedTasks,
			enabledReviewWords,
			disabledReviewWords
		);
	}

	public record AdminStatsResult(
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
	}
}
