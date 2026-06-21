package com.snk.server.api.controller;

import com.snk.server.api.dto.AdminFoodItemResponse;
import com.snk.server.api.dto.AdminFoodItemReportResponse;
import com.snk.server.api.dto.MergeFoodItemRequest;
import com.snk.server.api.dto.MergeFoodItemResponse;
import com.snk.server.domain.food.FoodFeedbackService;
import com.snk.server.domain.food.FoodModerationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/food-items")
@Validated
public class AdminFoodItemController {

	private final FoodModerationService foodModerationService;
	private final FoodFeedbackService foodFeedbackService;

	public AdminFoodItemController(FoodModerationService foodModerationService, FoodFeedbackService foodFeedbackService) {
		this.foodModerationService = foodModerationService;
		this.foodFeedbackService = foodFeedbackService;
	}

	@GetMapping
	public List<AdminFoodItemResponse> listFoodItems(
		@RequestParam(value = "auditStatus", required = false) String auditStatus,
		@RequestParam(value = "q", required = false) String query,
		@RequestParam(value = "limit", defaultValue = "20") int limit
	) {
		return foodModerationService.listFoodItems(auditStatus, query, limit)
			.stream()
			.map(AdminFoodItemResponse::from)
			.toList();
	}

	@GetMapping("/{foodItemId}")
	public AdminFoodItemResponse getFoodItem(@PathVariable("foodItemId") @Positive Long foodItemId) {
		return AdminFoodItemResponse.from(foodModerationService.getFoodItem(foodItemId));
	}

	@GetMapping("/{foodItemId}/reports")
	public List<AdminFoodItemReportResponse> listFoodItemReports(@PathVariable("foodItemId") @Positive Long foodItemId) {
		return foodFeedbackService.listFoodItemReports(foodItemId)
			.stream()
			.map(AdminFoodItemReportResponse::from)
			.toList();
	}

	@GetMapping("/pending")
	public List<AdminFoodItemResponse> listPendingItems() {
		return foodModerationService.listPendingItems()
			.stream()
			.map(AdminFoodItemResponse::from)
			.toList();
	}

	@GetMapping("/reported")
	public List<AdminFoodItemResponse> listReportedItems(
		@RequestParam(value = "minReportCount", defaultValue = "1") int minReportCount
	) {
		return foodModerationService.listReportedItems(minReportCount)
			.stream()
			.map(AdminFoodItemResponse::from)
			.toList();
	}

	@PostMapping("/{foodItemId}/approve")
	@ResponseStatus(HttpStatus.OK)
	public AdminFoodItemResponse approveFoodItem(@PathVariable("foodItemId") @Positive Long foodItemId) {
		return AdminFoodItemResponse.from(foodModerationService.approveFoodItem(foodItemId));
	}

	@PostMapping("/{foodItemId}/reject")
	@ResponseStatus(HttpStatus.OK)
	public AdminFoodItemResponse rejectFoodItem(@PathVariable("foodItemId") @Positive Long foodItemId) {
		return AdminFoodItemResponse.from(foodModerationService.rejectFoodItem(foodItemId));
	}

	@PostMapping("/{foodItemId}/clear-reports")
	@ResponseStatus(HttpStatus.OK)
	public AdminFoodItemResponse clearReportCount(@PathVariable("foodItemId") @Positive Long foodItemId) {
		return AdminFoodItemResponse.from(foodModerationService.clearReportCount(foodItemId));
	}

	@PostMapping("/{foodItemId}/merge")
	@ResponseStatus(HttpStatus.OK)
	public MergeFoodItemResponse mergeFoodItem(
		@PathVariable("foodItemId") @Positive Long foodItemId,
		@Valid @RequestBody MergeFoodItemRequest request
	) {
		return MergeFoodItemResponse.from(foodModerationService.mergeFoodItem(foodItemId, request.targetFoodItemId()));
	}
}
