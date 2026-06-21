package com.snk.server.domain.food;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.food.FoodItemReportEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemReportRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FoodFeedbackService {

	private final FoodItemRepository foodItemRepository;
	private final UserRepository userRepository;
	private final FoodItemReportRepository foodItemReportRepository;

	public FoodFeedbackService(
		FoodItemRepository foodItemRepository,
		UserRepository userRepository,
		FoodItemReportRepository foodItemReportRepository
	) {
		this.foodItemRepository = foodItemRepository;
		this.userRepository = userRepository;
		this.foodItemReportRepository = foodItemReportRepository;
	}

	@Transactional
	public FoodReportResult reportFoodItem(Long userId, Long foodItemId, String reason) {
		UserEntity reporter = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
		FoodItemEntity foodItem = foodItemRepository.findById(foodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));

		Integer currentReportCount = foodItem.getReportCount() == null ? 0 : foodItem.getReportCount();
		foodItem.setReportCount(currentReportCount + 1);
		FoodItemEntity saved = foodItemRepository.save(foodItem);
		String normalizedReason = normalizeReason(reason);
		FoodItemReportEntity report = new FoodItemReportEntity();
		report.setFoodItem(saved);
		report.setReporterUser(reporter);
		report.setReason(normalizedReason);
		foodItemReportRepository.save(report);

		return new FoodReportResult(
			saved.getId(),
			saved.getReportCount(),
			saved.getAuditStatus(),
			reporter.getId(),
			normalizedReason
		);
	}

	@Transactional(readOnly = true)
	public List<FoodItemReportItem> listFoodItemReports(Long foodItemId) {
		return foodItemReportRepository.findByFoodItem_IdOrderByCreatedAtDesc(foodItemId)
			.stream()
			.map(FoodFeedbackService::toReportItem)
			.toList();
	}

	private static FoodItemReportItem toReportItem(FoodItemReportEntity entity) {
		return new FoodItemReportItem(
			entity.getId(),
			entity.getFoodItem().getId(),
			entity.getReporterUser().getId(),
			entity.getReason(),
			entity.getCreatedAt()
		);
	}

	private String normalizeReason(String reason) {
		if (reason == null) {
			return null;
		}
		String normalized = reason.trim();
		return normalized.isBlank() ? null : normalized;
	}

	public record FoodReportResult(
		Long foodItemId,
		Integer reportCount,
		String auditStatus,
		Long reporterUserId,
		String reason
	) {
	}

	public record FoodItemReportItem(
		Long id,
		Long foodItemId,
		Long reporterUserId,
		String reason,
		OffsetDateTime createdAt
	) {
	}
}
