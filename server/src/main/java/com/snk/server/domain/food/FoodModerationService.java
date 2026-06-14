package com.snk.server.domain.food;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
