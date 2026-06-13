package com.snk.server.api.dto;

import java.util.List;

public record FoodSearchResponse(
	List<FoodSearchItemResponse> items,
	String qualitySignal
) {
}
