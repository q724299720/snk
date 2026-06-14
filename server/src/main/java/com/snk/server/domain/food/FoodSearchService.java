package com.snk.server.domain.food;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

	public FoodSearchResult recommendRelatedFoods(Long foodItemId, int limit) {
		int normalizedLimit = Math.min(Math.max(limit, 1), 10);
		FoodItemEntity seed = foodItemRepository.findById(foodItemId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));

		LinkedHashMap<Long, FoodSearchItem> items = new LinkedHashMap<>();
		for (String query : buildRecommendationQueries(seed)) {
			for (FoodItemEntity candidate : foodItemRepository.searchApproved(query)) {
				if (candidate.getId().equals(seed.getId())) {
					continue;
				}
				items.putIfAbsent(candidate.getId(), toItem(candidate));
				if (items.size() >= normalizedLimit) {
					return new FoodSearchResult(new ArrayList<>(items.values()), "related");
				}
			}
		}

		return new FoodSearchResult(new ArrayList<>(items.values()), items.isEmpty() ? "weak" : "related");
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
			entity.getCoverImageUrl(),
			entity.getAuditStatus()
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

	private List<String> buildRecommendationQueries(FoodItemEntity entity) {
		Set<String> queries = new LinkedHashSet<>();
		addQuery(queries, entity.getBrand());
		addQuery(queries, entity.getSubcategory());
		addQuery(queries, entity.getCategory());
		addQuery(queries, entity.getName());
		return new ArrayList<>(queries);
	}

	private void addQuery(Set<String> queries, String value) {
		String normalized = value == null ? null : value.trim();
		if (normalized != null && !normalized.isBlank()) {
			queries.add(normalized);
		}
	}
}
