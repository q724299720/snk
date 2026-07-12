package com.snk.server.domain.food;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FoodModerationService {
	private static final Set<String> ALLOWED_ITEM_TYPES = Set.of("packaged_product", "dish", "fruit", "unknown");

	private final FoodItemRepository foodItemRepository;
	private final FoodRecordRepository foodRecordRepository;

	public FoodModerationService(FoodItemRepository foodItemRepository, FoodRecordRepository foodRecordRepository) {
		this.foodItemRepository = foodItemRepository;
		this.foodRecordRepository = foodRecordRepository;
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
		return listFoodItems(auditStatus, query, null, null, limit);
	}

	@Transactional(readOnly = true)
	public List<FoodModerationItem> listFoodItems(
		String auditStatus,
		String query,
		String itemType,
		String category,
		int limit
	) {
		int normalizedLimit = Math.min(Math.max(limit, 1), 100);
		String normalizedAuditStatus = normalizeOptional(auditStatus);
		String normalizedQuery = normalizeOptional(query);
		String normalizedItemType = normalizeOptional(itemType);
		String normalizedCategory = normalizeOptional(category);
		return foodItemRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
			.stream()
			.filter(entity -> normalizedAuditStatus == null || normalizedAuditStatus.equals(entity.getAuditStatus()))
			.filter(entity -> normalizedItemType == null || normalizedItemType.equals(entity.getItemType()))
			.filter(entity -> normalizedCategory == null || normalizedCategory.equals(entity.getCategory()))
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
	public FoodModerationItem updateFoodItem(Long foodItemId, UpdateFoodItemCommand command) {
		FoodItemEntity entity = foodItemRepository.findById(foodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));
		String name = normalizeRequired(command.name(), "name");
		String itemType = normalizeRequired(command.itemType(), "itemType");
		if (!ALLOWED_ITEM_TYPES.contains(itemType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid food item type.");
		}
		entity.setName(name);
		entity.setItemType(itemType);
		entity.setCategory("none");
		entity.setSubcategory(null);
		entity.setBrand(normalizeOptional(command.brand()));
		entity.setAlias(normalizeOptional(command.alias()));
		entity.setSearchKeywords(normalizeOptional(command.searchKeywords()) == null ? name : normalizeOptional(command.searchKeywords()));
		return toModerationItem(foodItemRepository.save(entity));
	}

	@Transactional(readOnly = true)
	public List<FoodModerationItem> findMergeCandidates(Long foodItemId, int limit) {
		FoodItemEntity source = foodItemRepository.findById(foodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));
		return foodItemRepository.findMergeCandidates(source.getId(), source.getName(), Math.min(Math.max(limit, 1), 10))
			.stream()
			.map(this::toModerationItem)
			.toList();
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

	@Transactional
	public FoodModerationItem clearReportCount(Long foodItemId) {
		FoodItemEntity entity = foodItemRepository.findById(foodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));
		entity.setReportCount(0);
		return toModerationItem(foodItemRepository.save(entity));
	}

	@Transactional
	public FoodItemMergeResult mergeFoodItem(Long duplicateFoodItemId, Long targetFoodItemId) {
		if (duplicateFoodItemId.equals(targetFoodItemId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate and target food items must be different.");
		}
		FoodItemEntity duplicate = foodItemRepository.findById(duplicateFoodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Duplicate food item not found."));
		FoodItemEntity target = foodItemRepository.findById(targetFoodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target food item not found."));
		if (!"approved".equals(target.getAuditStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target food item must be approved before merge.");
		}

		int migratedRecordCount = foodRecordRepository.reassignFoodItem(duplicate, target);
		duplicate.setAuditStatus("rejected");
		duplicate.setReportCount(0);
		FoodItemEntity savedDuplicate = foodItemRepository.save(duplicate);
		return new FoodItemMergeResult(
			toModerationItem(savedDuplicate),
			toModerationItem(target),
			migratedRecordCount
		);
	}

	private FoodModerationItem toModerationItem(FoodItemEntity entity) {
		Long createdByUserId = entity.getCreatedByUser() == null ? null : entity.getCreatedByUser().getId();
		return new FoodModerationItem(
			entity.getId(),
			entity.getName(),
			entity.getName().trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT),
			entity.getItemType(),
			entity.getCategory(),
			entity.getSubcategory(),
			entity.getBrand(),
			entity.getBarcode(),
			entity.getSource(),
			entity.getAuditStatus(),
			entity.getReportCount(),
			foodRecordRepository.countByFoodItem_Id(entity.getId()),
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

	@Transactional
	public FoodModerationItem hideFoodItem(Long foodItemId) {
		FoodItemEntity entity = foodItemRepository.findById(foodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));
		entity.setSearchable(false);
		return toModerationItem(foodItemRepository.save(entity));
	}

	@Transactional
	public FoodModerationItem restoreFoodItem(Long foodItemId) {
		FoodItemEntity entity = foodItemRepository.findById(foodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));
		entity.setSearchable(true);
		return toModerationItem(foodItemRepository.save(entity));
	}

	private String normalizeRequired(String value, String fieldName) {
		String normalized = normalizeOptional(value);
		if (normalized == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must not be blank");
		}
		return normalized;
	}

	public record FoodModerationItem(
		Long id,
		String name,
		String normalizedName,
		String itemType,
		String category,
		String subcategory,
		String brand,
		String barcode,
		String source,
		String auditStatus,
		Integer reportCount,
		long recordCount,
		Long createdByUserId,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
	) {
	}

	public record FoodItemMergeResult(
		FoodModerationItem duplicateItem,
		FoodModerationItem targetItem,
		int migratedRecordCount
	) {
	}
}
