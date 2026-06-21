package com.snk.server.api.controller;

import com.snk.server.api.dto.CreateManualFoodItemRequest;
import com.snk.server.api.dto.FoodSearchItemResponse;
import com.snk.server.api.dto.FoodSearchResponse;
import com.snk.server.domain.food.CreateManualFoodItemCommand;
import com.snk.server.domain.food.FoodSearchResult;
import com.snk.server.domain.food.FoodSearchService;
import com.snk.server.domain.food.ManualFoodItemService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/foods")
public class FoodSearchController {

	private final FoodSearchService foodSearchService;
	private final ManualFoodItemService manualFoodItemService;

	public FoodSearchController(FoodSearchService foodSearchService, ManualFoodItemService manualFoodItemService) {
		this.foodSearchService = foodSearchService;
		this.manualFoodItemService = manualFoodItemService;
	}

	@GetMapping("/search")
	public FoodSearchResponse search(
		@RequestParam("q") String query,
		@RequestParam(value = "userId", required = false) Long userId
	) {
		String normalizedQuery = query == null ? "" : query.trim();
		if (normalizedQuery.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be blank");
		}
		FoodSearchResult result = userId == null
			? foodSearchService.search(normalizedQuery)
			: foodSearchService.search(normalizedQuery, userId);
		List<FoodSearchItemResponse> items = result.items().stream()
			.map(FoodSearchItemResponse::from)
			.toList();
		return new FoodSearchResponse(items, result.qualitySignal());
	}

	@GetMapping("/barcode/{code}")
	public FoodSearchItemResponse lookupByBarcode(@PathVariable("code") String code) {
		String normalizedCode = code == null ? "" : code.trim();
		if (normalizedCode.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "barcode must not be blank");
		}
		return foodSearchService.lookupByBarcode(normalizedCode)
			.map(FoodSearchItemResponse::from)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "food not found"));
	}

	@GetMapping("/{foodItemId}/related")
	public FoodSearchResponse related(@PathVariable("foodItemId") Long foodItemId) {
		FoodSearchResult result = foodSearchService.recommendRelatedFoods(foodItemId, 5);
		List<FoodSearchItemResponse> items = result.items().stream()
			.map(FoodSearchItemResponse::from)
			.toList();
		return new FoodSearchResponse(items, result.qualitySignal());
	}

	@PostMapping("/manual")
	@ResponseStatus(HttpStatus.CREATED)
	public FoodSearchItemResponse createManualFoodItem(@Valid @RequestBody CreateManualFoodItemRequest request) {
		var item = manualFoodItemService.createPendingItem(
			new CreateManualFoodItemCommand(
				request.userId(),
				request.name(),
				request.itemType(),
				request.category(),
				request.subcategory(),
				request.brand(),
				request.barcode()
			)
		);
		return FoodSearchItemResponse.from(item);
	}
}
