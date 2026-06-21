package com.snk.server.api.controller;

import com.snk.server.api.dto.CreateFoodRecordRequest;
import com.snk.server.api.dto.CreateFoodRecordCommentRequest;
import com.snk.server.api.dto.FoodRecordCommentResponse;
import com.snk.server.api.dto.FoodRecordHistoryResponse;
import com.snk.server.api.dto.FoodRecordImageResponse;
import com.snk.server.api.dto.FoodRecordResponse;
import com.snk.server.api.dto.UpdateFoodRecordRequest;
import com.snk.server.domain.record.FoodRecordCreateCommand;
import com.snk.server.domain.record.FoodRecordImageValue;
import com.snk.server.domain.record.FoodRecordResult;
import com.snk.server.domain.record.FoodRecordService;
import com.snk.server.domain.record.FoodRecordUpdateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	@GetMapping("/public")
	public List<FoodRecordHistoryResponse> listPublicRecords(
		@RequestParam(value = "limit", defaultValue = "10") @Positive int limit
	) {
		return foodRecordService.listPublicRecords(limit).stream()
			.map(FoodRecordHistoryResponse::from)
			.toList();
	}

	@GetMapping("/{recordId}")
	public FoodRecordResponse getRecord(
		@PathVariable @Positive Long recordId,
		@RequestParam("userId") @Positive Long userId
	) {
		return toResponse(foodRecordService.getRecordForUser(recordId, userId));
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
				request.recordTime(),
				request.imagesOrEmpty().stream()
					.map(image -> new FoodRecordImageValue(image.imageUrl(), image.thumbnailUrl()))
					.toList()
			)
		);
		return toResponse(result);
	}

	@PutMapping("/{recordId}")
	public FoodRecordResponse updateRecord(
		@PathVariable @Positive Long recordId,
		@Valid @RequestBody UpdateFoodRecordRequest request
	) {
		return toResponse(
			foodRecordService.updateRecord(
				new FoodRecordUpdateCommand(
					recordId,
					request.userId(),
					request.rating(),
					request.comment(),
					request.isPublic(),
					request.imagesOrEmpty().stream()
						.map(image -> new FoodRecordImageValue(image.imageUrl(), image.thumbnailUrl()))
						.toList()
				)
			)
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
		return toResponse(result);
	}

	@GetMapping("/{recordId}/comments")
	public List<FoodRecordCommentResponse> listComments(
		@PathVariable @Positive Long recordId,
		@RequestParam(value = "limit", defaultValue = "10") @Positive int limit
	) {
		return foodRecordService.listComments(recordId, limit).stream()
			.map(FoodRecordCommentResponse::from)
			.toList();
	}

	@PostMapping("/{recordId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public FoodRecordCommentResponse createComment(
		@PathVariable @Positive Long recordId,
		@Valid @RequestBody CreateFoodRecordCommentRequest request
	) {
		return FoodRecordCommentResponse.from(
			foodRecordService.createComment(recordId, request.userId(), request.content())
		);
	}

	private FoodRecordResponse toResponse(FoodRecordResult result) {
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
			result.createdAt(),
			result.images().stream()
				.map(FoodRecordImageResponse::from)
				.toList()
		);
	}
}
