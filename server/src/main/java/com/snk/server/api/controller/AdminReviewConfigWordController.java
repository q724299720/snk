package com.snk.server.api.controller;

import com.snk.server.api.dto.CreateReviewConfigWordRequest;
import com.snk.server.api.dto.ReviewConfigWordAuditLogResponse;
import com.snk.server.api.dto.ReviewConfigWordResponse;
import com.snk.server.api.dto.UpdateReviewConfigWordRequest;
import com.snk.server.domain.review.CreateReviewConfigWordCommand;
import com.snk.server.domain.review.ReviewConfigWordService;
import com.snk.server.domain.review.UpdateReviewConfigWordCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/review-config-words")
public class AdminReviewConfigWordController {

	private final ReviewConfigWordService reviewConfigWordService;

	public AdminReviewConfigWordController(ReviewConfigWordService reviewConfigWordService) {
		this.reviewConfigWordService = reviewConfigWordService;
	}

	@GetMapping
	public List<ReviewConfigWordResponse> listWords(
		@RequestParam(value = "enabled", required = false) Boolean enabled,
		@RequestParam(value = "wordType", required = false) String wordType
	) {
		return reviewConfigWordService.listWords(enabled, wordType)
			.stream()
			.map(ReviewConfigWordResponse::from)
			.toList();
	}

	@GetMapping("/{wordId}/audit-logs")
	public List<ReviewConfigWordAuditLogResponse> listAuditLogs(@PathVariable("wordId") Long wordId) {
		return reviewConfigWordService.listAuditLogs(wordId)
			.stream()
			.map(ReviewConfigWordAuditLogResponse::from)
			.toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewConfigWordResponse createWord(@Valid @RequestBody CreateReviewConfigWordRequest request) {
		return ReviewConfigWordResponse.from(reviewConfigWordService.createWord(
			new CreateReviewConfigWordCommand(
				request.word(),
				request.wordType(),
				request.source(),
				request.remark(),
				request.enabled(),
				request.operatorId(),
				request.operatorName()
			)
		));
	}

	@PutMapping("/{wordId}")
	public ReviewConfigWordResponse updateWord(
		@PathVariable("wordId") Long wordId,
		@Valid @RequestBody UpdateReviewConfigWordRequest request
	) {
		return ReviewConfigWordResponse.from(reviewConfigWordService.updateWord(
			new UpdateReviewConfigWordCommand(
				wordId,
				request.word(),
				request.wordType(),
				request.source(),
				request.remark(),
				request.operatorId(),
				request.operatorName()
			)
		));
	}

	@PostMapping("/{wordId}/enable")
	public ReviewConfigWordResponse enableWord(
		@PathVariable("wordId") Long wordId,
		@Valid @RequestBody ReviewConfigWordOperatorRequest request
	) {
		return ReviewConfigWordResponse.from(reviewConfigWordService.enableWord(wordId, request.operatorId(), request.operatorName()));
	}

	@PostMapping("/{wordId}/disable")
	public ReviewConfigWordResponse disableWord(
		@PathVariable("wordId") Long wordId,
		@Valid @RequestBody ReviewConfigWordOperatorRequest request
	) {
		return ReviewConfigWordResponse.from(reviewConfigWordService.disableWord(wordId, request.operatorId(), request.operatorName()));
	}

	public record ReviewConfigWordOperatorRequest(
		@jakarta.validation.constraints.NotBlank
		@jakarta.validation.constraints.Size(max = 64)
		String operatorId,
		@jakarta.validation.constraints.NotBlank
		@jakarta.validation.constraints.Size(max = 128)
		String operatorName
	) {
	}
}
