package com.snk.server.infrastructure.persistence.recognition;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "recognition_tasks")
public class RecognitionTaskEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(name = "input_image_url", nullable = false, length = 512)
	private String inputImageUrl;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(name = "top_candidates", nullable = false, columnDefinition = "jsonb")
	private String topCandidates = "[]";

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "selected_food_item_id")
	private FoodItemEntity selectedFoodItem;

	@Column(precision = 5, scale = 4)
	private BigDecimal confidence;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "finished_at")
	private OffsetDateTime finishedAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
		if (topCandidates == null || topCandidates.isBlank()) {
			topCandidates = "[]";
		}
	}

	@PreUpdate
	void onUpdate() {
		if (topCandidates == null || topCandidates.isBlank()) {
			topCandidates = "[]";
		}
	}

	public Long getId() {
		return id;
	}

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public String getInputImageUrl() {
		return inputImageUrl;
	}

	public void setInputImageUrl(String inputImageUrl) {
		this.inputImageUrl = inputImageUrl;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTopCandidates() {
		return topCandidates;
	}

	public void setTopCandidates(String topCandidates) {
		this.topCandidates = topCandidates;
	}

	public FoodItemEntity getSelectedFoodItem() {
		return selectedFoodItem;
	}

	public void setSelectedFoodItem(FoodItemEntity selectedFoodItem) {
		this.selectedFoodItem = selectedFoodItem;
	}

	public BigDecimal getConfidence() {
		return confidence;
	}

	public void setConfidence(BigDecimal confidence) {
		this.confidence = confidence;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(OffsetDateTime finishedAt) {
		this.finishedAt = finishedAt;
	}
}
