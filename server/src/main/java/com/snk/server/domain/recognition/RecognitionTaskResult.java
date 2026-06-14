package com.snk.server.domain.recognition;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record RecognitionTaskResult(
	Long id,
	Long userId,
	String inputImageUrl,
	String status,
	List<ImageRecognitionCandidateSnapshot> topCandidates,
	Long selectedFoodItemId,
	BigDecimal confidence,
	OffsetDateTime createdAt,
	OffsetDateTime finishedAt,
	String statusReason
) {
}
