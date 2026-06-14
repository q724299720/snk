package com.snk.server.api.controller;

import com.snk.server.api.dto.FoodSearchItemResponse;
import com.snk.server.api.dto.FoodSearchResponse;
import com.snk.server.domain.food.FoodSearchResult;
import com.snk.server.domain.food.FoodSearchService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/foods")
public class FoodSearchController {

	private final FoodSearchService foodSearchService;

	public FoodSearchController(FoodSearchService foodSearchService) {
		this.foodSearchService = foodSearchService;
	}

	@GetMapping("/search")
	public FoodSearchResponse search(@RequestParam("q") String query) {
		String normalizedQuery = query == null ? "" : query.trim();
		if (normalizedQuery.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be blank");
		}
		FoodSearchResult result = foodSearchService.search(normalizedQuery);
		List<FoodSearchItemResponse> items = result.items().stream()
			.map(item -> new FoodSearchItemResponse(
				item.id(),
				item.name(),
				item.itemType(),
				item.category(),
				item.subcategory(),
				item.brand(),
				item.barcode(),
				item.coverImageUrl()
			))
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
			.map(item -> new FoodSearchItemResponse(
				item.id(),
				item.name(),
				item.itemType(),
				item.category(),
				item.subcategory(),
				item.brand(),
				item.barcode(),
				item.coverImageUrl()
			))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "food not found"));
	}
}
