package com.snk.server.domain.recognition;

public record ImageRecognitionCandidateSnapshot(
	Long foodItemId,
	String name,
	String itemType,
	String category,
	String subcategory,
	String brand,
	String barcode,
	String coverImageUrl,
	String auditStatus
) {
}
