package com.snk.server.domain.food;

import java.util.List;

public record FoodSearchResult(
	List<FoodSearchItem> items,
	String qualitySignal
) {
}
