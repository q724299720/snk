package com.snk.server.infrastructure.persistence.record;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "food_record_images")
public class FoodRecordImageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "record_id", nullable = false)
	private FoodRecordEntity record;

	@Column(name = "image_url", nullable = false, length = 512)
	private String imageUrl;

	@Column(name = "thumbnail_url", length = 512)
	private String thumbnailUrl;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public FoodRecordEntity getRecord() {
		return record;
	}

	public void setRecord(FoodRecordEntity record) {
		this.record = record;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
