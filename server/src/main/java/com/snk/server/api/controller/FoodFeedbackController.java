package com.snk.server.api.controller;

import com.snk.server.api.dto.CreateFoodReportRequest;
import com.snk.server.api.dto.FoodReportResponse;
import com.snk.server.domain.food.FoodFeedbackService;
import com.snk.server.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/foods")
@Validated
public class FoodFeedbackController {

	private final FoodFeedbackService foodFeedbackService;
	private final CurrentUser currentUser;

	public FoodFeedbackController(FoodFeedbackService foodFeedbackService, CurrentUser currentUser) {
		this.foodFeedbackService = foodFeedbackService;
		this.currentUser = currentUser;
	}

	@PostMapping("/{foodItemId}/report")
	@ResponseStatus(HttpStatus.CREATED)
	public FoodReportResponse reportFoodItem(
		@PathVariable("foodItemId") @Positive Long foodItemId,
		@Valid @RequestBody CreateFoodReportRequest request
	) {
		var result = foodFeedbackService.reportFoodItem(currentUser.requiredUserId(), foodItemId, request.reason());
		return new FoodReportResponse(result.foodItemId(), result.reportCount(), result.auditStatus());
	}
}
