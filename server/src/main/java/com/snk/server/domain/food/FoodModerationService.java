package com.snk.server.domain.food;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FoodModerationService {

	private final FoodItemRepository foodItemRepository;

	public FoodModerationService(FoodItemRepository foodItemRepository) {
		this.foodItemRepository = foodItemRepository;
	}

	@Transactional(readOnly = true)
	public List<FoodModerationItem> listPendingItems() {
		return foodItemRepository.findByAuditStatusOrderByCreatedAtDesc("pending")
			.stream()
			.map(this::toModerationItem)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<FoodModerationItem> listReportedItems(int minReportCount) {
		int normalizedMinReportCount = Math.max(minReportCount, 1);
		return foodItemRepository.findByReportCountGreaterThanEqualOrderByReportCountDescCreatedAtDesc(normalizedMinReportCount)
			.stream()
			.map(this::toModerationItem)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<FoodModerationItem> listFoodItems(String auditStatus, String query, int limit) {
		int normalizedLimit = Math.min(Math.max(limit, 1), 100);
		String normalizedAuditStatus = normalizeOptional(auditStatus);
		String normalizedQuery = normalizeOptional(query);
		return foodItemRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
			.stream()
			.filter(entity -> normalizedAuditStatus == null || normalizedAuditStatus.equals(entity.getAuditStatus()))
			.filter(entity -> normalizedQuery == null || matchesQuery(entity, normalizedQuery))
			.limit(normalizedLimit)
			.map(this::toModerationItem)
			.toList();
	}

	@Transactional(readOnly = true)
	public FoodModerationItem getFoodItem(Long foodItemId) {
		FoodItemEntity entity = foodItemRepository.findById(foodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));
		return toModerationItem(entity);
	}

	@Transactional
	public FoodModerationItem approveFoodItem(Long foodItemId) {
		FoodItemEntity entity = foodItemRepository.findById(foodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));
		entity.setAuditStatus("approved");
		return toModerationItem(foodItemRepository.save(entity));
	}

	@Transactional
	public FoodModerationItem rejectFoodItem(Long foodItemId) {
		FoodItemEntity entity = foodItemRepository.findById(foodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));
		entity.setAuditStatus("rejected");
		return toModerationItem(foodItemRepository.save(entity));
	}

	private FoodModerationItem toModerationItem(FoodItemEntity entity) {
		Long createdByUserId = entity.getCreatedByUser() == null ? null : entity.getCreatedByUser().getId();
		return new FoodModerationItem(
			entity.getId(),
			entity.getName(),
			entity.getItemType(),
			entity.getCategory(),
			entity.getSubcategory(),
			entity.getBrand(),
			entity.getBarcode(),
			entity.getSource(),
			entity.getAuditStatus(),
			entity.getReportCount(),
			createdByUserId,
			entity.getCreatedAt(),
			entity.getUpdatedAt()
		);
	}

	private boolean matchesQuery(FoodItemEntity entity, String query) {
		String lowerQuery = query.toLowerCase();
		return contains(entity.getName(), lowerQuery)
			|| contains(entity.getBrand(), lowerQuery)
			|| contains(entity.getCategory(), lowerQuery)
			|| contains(entity.getSubcategory(), lowerQuery)
			|| contains(entity.getBarcode(), lowerQuery)
			|| contains(entity.getSearchKeywords(), lowerQuery);
	}

	private boolean contains(String value, String lowerQuery) {
		return value != null && value.toLowerCase().contains(lowerQuery);
	}

	private String normalizeOptional(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isBlank() ? null : normalized;
	}

	public record FoodModerationItem(
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
	}
}
