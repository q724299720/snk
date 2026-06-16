package com.snk.server.api.dto;

import com.snk.server.domain.recognition.RecognitionTaskResult;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record RecognitionTaskResponse(
	Long id,
	Long userId,
	String inputImageUrl,
	String status,
	List<FoodSearchItemResponse> topCandidates,
	Long selectedFoodItemId,
	BigDecimal confidence,
	OffsetDateTime createdAt,
	OffsetDateTime finishedAt,
	String statusReason
) {

	public static RecognitionTaskResponse from(RecognitionTaskResult result) {
		return new RecognitionTaskResponse(
			result.id(),
			result.userId(),
			result.inputImageUrl(),
			result.status(),
			result.topCandidates().stream()
				.map(candidate -> new FoodSearchItemResponse(
					candidate.foodItemId(),
					candidate.name(),
					candidate.itemType(),
					candidate.category(),
					candidate.subcategory(),
					candidate.brand(),
					candidate.barcode(),
					candidate.coverImageUrl(),
					null,
					candidate.auditStatus()
				))
				.toList(),
			result.selectedFoodItemId(),
			result.confidence(),
			result.createdAt(),
			result.finishedAt(),
			result.statusReason()
		);
	}
}
