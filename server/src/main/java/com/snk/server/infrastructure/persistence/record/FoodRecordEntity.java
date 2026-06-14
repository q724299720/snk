package com.snk.server.infrastructure.persistence.record;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "food_records")
public class FoodRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "food_item_id", nullable = false)
	private FoodItemEntity foodItem;

	@Column(name = "source_type", nullable = false, length = 32)
	private String sourceType;

	@Column(name = "sync_status", nullable = false, length = 32)
	private String syncStatus;

	@Column(name = "is_public", nullable = false)
	private boolean isPublic;

	@Column(nullable = false)
	private short rating;

	@Column
	private String comment;

	@Column(name = "like_count", nullable = false)
	private int likeCount;

	@Column
	private BigDecimal price;

	@Column(length = 255)
	private String location;

	@Column(name = "record_time", nullable = false)
	private OffsetDateTime recordTime;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (recordTime == null) {
			recordTime = now;
		}
		if (createdAt == null) {
			createdAt = now;
		}
		if (syncStatus == null) {
			syncStatus = "synced";
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

	public FoodItemEntity getFoodItem() {
		return foodItem;
	}

	public void setFoodItem(FoodItemEntity foodItem) {
		this.foodItem = foodItem;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public String getSyncStatus() {
		return syncStatus;
	}

	public void setSyncStatus(String syncStatus) {
		this.syncStatus = syncStatus;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean aPublic) {
		isPublic = aPublic;
	}

	public short getRating() {
		return rating;
	}

	public void setRating(short rating) {
		this.rating = rating;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public int getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(int likeCount) {
		this.likeCount = likeCount;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public OffsetDateTime getRecordTime() {
		return recordTime;
	}

	public void setRecordTime(OffsetDateTime recordTime) {
		this.recordTime = recordTime;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}
}
