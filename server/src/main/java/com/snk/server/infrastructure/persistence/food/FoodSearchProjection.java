package com.snk.server.infrastructure.persistence.food;

import java.math.BigDecimal;

public interface FoodSearchProjection {

	Long getId();

	String getName();

	String getItemType();

	String getCategory();

	String getSubcategory();

	String getBrand();

	String getBarcode();

	String getCoverImageUrl();

	String getAuditStatus();

	BigDecimal getAverageRating();
}
