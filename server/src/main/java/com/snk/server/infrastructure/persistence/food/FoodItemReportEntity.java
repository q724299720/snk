package com.snk.server.infrastructure.persistence.food;

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
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "food_item_reports")
public class FoodItemReportEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "food_item_id", nullable = false)
	private FoodItemEntity foodItem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_user_id", nullable = false)
	private UserEntity reporterUser;

	@Column(length = 2000)
	private String reason;

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

	public FoodItemEntity getFoodItem() {
		return foodItem;
	}

	public void setFoodItem(FoodItemEntity foodItem) {
		this.foodItem = foodItem;
	}

	public UserEntity getReporterUser() {
		return reporterUser;
	}

	public void setReporterUser(UserEntity reporterUser) {
		this.reporterUser = reporterUser;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
