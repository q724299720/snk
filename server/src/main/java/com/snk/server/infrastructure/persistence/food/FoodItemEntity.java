package com.snk.server.infrastructure.persistence.food;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "food_items")
public class FoodItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(name = "item_type", nullable = false, length = 32)
	private String itemType;

	@Column(length = 64, nullable = false)
	private String category;

	@Column(length = 64)
	private String subcategory;

	@Column(length = 128)
	private String brand;

	@Column(length = 64)
	private String barcode;

	@Column(name = "audit_status", nullable = false, length = 32)
	private String auditStatus;

	@Column(name = "search_keywords", nullable = false)
	private String searchKeywords;

	@Column(name = "cover_image_url", length = 512)
	private String coverImageUrl;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getItemType() {
		return itemType;
	}

	public String getCategory() {
		return category;
	}

	public String getSubcategory() {
		return subcategory;
	}

	public String getBrand() {
		return brand;
	}

	public String getBarcode() {
		return barcode;
	}

	public String getAuditStatus() {
		return auditStatus;
	}

	public String getSearchKeywords() {
		return searchKeywords;
	}

	public String getCoverImageUrl() {
		return coverImageUrl;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
