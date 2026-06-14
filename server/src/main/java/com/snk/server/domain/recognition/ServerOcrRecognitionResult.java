package com.snk.server.domain.recognition;

import com.snk.server.domain.food.FoodSearchItem;
import java.util.List;

public record ServerOcrRecognitionResult(
	String recognizedText,
	List<String> attemptedQueries,
	String matchedQuery,
	String qualitySignal,
	List<FoodSearchItem> items
) {
}
