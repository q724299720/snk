package com.snk.server.api.controller;

import com.snk.server.api.dto.CreateFoodRecordRequest;
import com.snk.server.api.dto.FoodRecordResponse;
import com.snk.server.domain.record.FoodRecordCreateCommand;
import com.snk.server.domain.record.FoodRecordResult;
import com.snk.server.domain.record.FoodRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
public class FoodRecordController {

	private final FoodRecordService foodRecordService;

	public FoodRecordController(FoodRecordService foodRecordService) {
		this.foodRecordService = foodRecordService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public FoodRecordResponse createRecord(@Valid @RequestBody CreateFoodRecordRequest request) {
		FoodRecordResult result = foodRecordService.createRecord(
			new FoodRecordCreateCommand(
				request.userId(),
				request.foodItemId(),
				request.sourceType(),
				request.isPublic(),
				request.rating(),
				request.comment(),
				request.recordTime()
			)
		);
		return new FoodRecordResponse(
			result.id(),
			result.userId(),
			result.foodItemId(),
			result.sourceType(),
			result.isPublic(),
			result.rating(),
			result.comment(),
			result.recordTime(),
			result.createdAt()
		);
	}
}
