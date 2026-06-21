package com.snk.server.api.controller;

import com.snk.server.api.dto.CreateFoodRecordRequest;
import com.snk.server.api.dto.FoodRecordHistoryResponse;
import com.snk.server.api.dto.FoodRecordResponse;
import com.snk.server.domain.record.FoodRecordCreateCommand;
import com.snk.server.domain.record.FoodRecordResult;
import com.snk.server.domain.record.FoodRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
public class FoodRecordController {

	private static final Set<String> ALLOWED_SOURCE_TYPES = Set.of("text_search", "manual");

	private final FoodRecordService foodRecordService;

	public FoodRecordController(FoodRecordService foodRecordService) {
		this.foodRecordService = foodRecordService;
	}

	@GetMapping
	public List<FoodRecordHistoryResponse> listRecentRecords(
		@RequestParam("userId") @Positive Long userId,
		@RequestParam(value = "limit", defaultValue = "10") @Positive int limit
	) {
		return foodRecordService.listRecentRecords(userId, limit).stream()
			.map(FoodRecordHistoryResponse::from)
			.toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public FoodRecordResponse createRecord(@Valid @RequestBody CreateFoodRecordRequest request) {
		FoodRecordResult result = foodRecordService.createRecord(
			new FoodRecordCreateCommand(
				request.userId(),
				request.foodItemId(),
				validateSourceType(request.sourceType()),
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
			result.likeCount(),
			result.recordTime(),
			result.createdAt()
		);
	}

	private String validateSourceType(String sourceType) {
		String normalizedSourceType = sourceType.trim();
		if (!ALLOWED_SOURCE_TYPES.contains(normalizedSourceType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid record source type.");
		}
		return normalizedSourceType;
	}

	@PostMapping("/{recordId}/like")
	@ResponseStatus(HttpStatus.OK)
	public FoodRecordResponse likeRecord(@PathVariable @Positive Long recordId) {
		FoodRecordResult result = foodRecordService.likeRecord(recordId);
		return new FoodRecordResponse(
			result.id(),
			result.userId(),
			result.foodItemId(),
			result.sourceType(),
			result.isPublic(),
			result.rating(),
			result.comment(),
			result.likeCount(),
			result.recordTime(),
			result.createdAt()
		);
	}
}
