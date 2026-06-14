package com.snk.server.domain.food;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FoodSearchService {

	private final FoodItemRepository foodItemRepository;

	public FoodSearchService(FoodItemRepository foodItemRepository) {
		this.foodItemRepository = foodItemRepository;
	}

	public FoodSearchResult search(String rawQuery) {
		String query = rawQuery == null ? "" : rawQuery.trim();
		List<FoodSearchItem> items = foodItemRepository.searchApproved(query).stream()
			.map(this::toItem)
			.toList();
		return new FoodSearchResult(items, resolveQualitySignal(query, items));
	}

	public Optional<FoodSearchItem> lookupByBarcode(String rawBarcode) {
		String barcode = rawBarcode == null ? "" : rawBarcode.trim();
		if (barcode.isBlank()) {
			return Optional.empty();
		}
		return foodItemRepository.findByAuditStatusAndBarcode("approved", barcode)
			.map(this::toItem);
	}

	private FoodSearchItem toItem(FoodItemEntity entity) {
		return new FoodSearchItem(
			entity.getId(),
			entity.getName(),
			entity.getItemType(),
			entity.getCategory(),
			entity.getSubcategory(),
			entity.getBrand(),
			entity.getBarcode(),
			entity.getCoverImageUrl()
		);
	}

	private String resolveQualitySignal(String query, List<FoodSearchItem> items) {
		if (items.isEmpty()) {
			return "weak";
		}
		FoodSearchItem top = items.getFirst();
		String normalizedQuery = query.toLowerCase();
		String normalizedName = top.name().toLowerCase();
		if (normalizedName.equals(normalizedQuery) || normalizedName.startsWith(normalizedQuery)) {
			return "strong";
		}
		return "weak";
	}
}
