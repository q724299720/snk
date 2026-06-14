package com.snk.server.api.controller;

import com.snk.server.api.dto.CreateFoodReportRequest;
import com.snk.server.api.dto.FoodReportResponse;
import com.snk.server.domain.food.FoodFeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/foods")
public class FoodFeedbackController {

	private final FoodFeedbackService foodFeedbackService;

	public FoodFeedbackController(FoodFeedbackService foodFeedbackService) {
		this.foodFeedbackService = foodFeedbackService;
	}

	@PostMapping("/{foodItemId}/report")
	@ResponseStatus(HttpStatus.CREATED)
	public FoodReportResponse reportFoodItem(
		@PathVariable("foodItemId") Long foodItemId,
		@Valid @RequestBody CreateFoodReportRequest request
	) {
		var result = foodFeedbackService.reportFoodItem(request.userId(), foodItemId, request.reason());
		return new FoodReportResponse(result.foodItemId(), result.reportCount(), result.auditStatus());
	}
}
