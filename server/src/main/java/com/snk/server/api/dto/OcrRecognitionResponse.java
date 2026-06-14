package com.snk.server.api.dto;

import java.util.List;

public record OcrRecognitionResponse(
	String recognizedText,
	List<String> attemptedQueries,
	String matchedQuery,
	String qualitySignal,
	List<FoodSearchItemResponse> items
) {
}
