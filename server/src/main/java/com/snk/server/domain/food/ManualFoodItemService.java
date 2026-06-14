package com.snk.server.domain.food;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManualFoodItemService {

	private static final Set<String> ALLOWED_ITEM_TYPES = Set.of("packaged_product", "dish", "fruit");

	private final FoodItemRepository foodItemRepository;
	private final UserRepository userRepository;

	public ManualFoodItemService(FoodItemRepository foodItemRepository, UserRepository userRepository) {
		this.foodItemRepository = foodItemRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public FoodSearchItem createPendingItem(CreateManualFoodItemCommand command) {
		UserEntity creator = userRepository.findById(command.userId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
		String itemType = normalizeRequired(command.itemType(), "itemType");
		if (!ALLOWED_ITEM_TYPES.contains(itemType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemType is invalid");
		}
		String barcode = normalizeBarcode(command.barcode());
		if (barcode != null) {
			if (!"packaged_product".equals(itemType)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "barcode is only supported for packaged_product");
			}
			if (foodItemRepository.findFirstByItemTypeAndBarcode("packaged_product", barcode).isPresent()) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "barcode already exists");
			}
		}

		FoodItemEntity entity = new FoodItemEntity();
		entity.setName(normalizeRequired(command.name(), "name"));
		entity.setItemType(itemType);
		entity.setCategory(normalizeRequired(command.category(), "category"));
		entity.setSubcategory(normalizeOptional(command.subcategory()));
		entity.setBrand(normalizeOptional(command.brand()));
		entity.setBarcode(barcode);
		entity.setSource("user_generated");
		entity.setAuditStatus("pending");
		entity.setSearchKeywords(buildSearchKeywords(entity));
		entity.setReportCount(0);
		entity.setCoverImageUrl(null);
		entity.setCreatedByUser(creator);

		FoodItemEntity saved = foodItemRepository.save(entity);
		return new FoodSearchItem(
			saved.getId(),
			saved.getName(),
			saved.getItemType(),
			saved.getCategory(),
			saved.getSubcategory(),
			saved.getBrand(),
			saved.getBarcode(),
			saved.getCoverImageUrl(),
			saved.getAuditStatus()
		);
	}

	private String buildSearchKeywords(FoodItemEntity entity) {
		LinkedHashSet<String> parts = new LinkedHashSet<>();
		addKeyword(parts, entity.getName());
		addKeyword(parts, entity.getBrand());
		addKeyword(parts, entity.getCategory());
		addKeyword(parts, entity.getSubcategory());
		addKeyword(parts, entity.getBarcode());
		return String.join(" ", parts);
	}

	private void addKeyword(Set<String> parts, String value) {
		String normalized = normalizeOptional(value);
		if (normalized == null) {
			return;
		}
		parts.add(normalized);
		String merged = normalized.replace(" ", "");
		if (!merged.isBlank()) {
			parts.add(merged);
		}
	}

	private String normalizeRequired(String value, String fieldName) {
		String normalized = normalizeOptional(value);
		if (normalized == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must not be blank");
		}
		return normalized;
	}

	private String normalizeOptional(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim().replaceAll("\\s+", " ");
		return normalized.isBlank() ? null : normalized;
	}

	private String normalizeBarcode(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.replaceAll("\\s+", "").trim();
		return normalized.isBlank() ? null : normalized;
	}
}
