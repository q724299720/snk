package com.snk.server.infrastructure.persistence.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "review_config_word_audit_logs")
public class ReviewConfigWordAuditLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "review_config_word_id", nullable = false)
	private Long reviewConfigWordId;

	@Column(name = "action_type", nullable = false, length = 32)
	private String actionType;

	@Column(name = "before_value", columnDefinition = "jsonb")
	private String beforeValue;

	@Column(name = "after_value", columnDefinition = "jsonb")
	private String afterValue;

	@Column(name = "operator_id", length = 64)
	private String operatorId;

	@Column(name = "operator_name", length = 128)
	private String operatorName;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	public Long getId() {
		return id;
	}

	public Long getReviewConfigWordId() {
		return reviewConfigWordId;
	}

	public void setReviewConfigWordId(Long reviewConfigWordId) {
		this.reviewConfigWordId = reviewConfigWordId;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public String getBeforeValue() {
		return beforeValue;
	}

	public void setBeforeValue(String beforeValue) {
		this.beforeValue = beforeValue;
	}

	public String getAfterValue() {
		return afterValue;
	}

	public void setAfterValue(String afterValue) {
		this.afterValue = afterValue;
	}

	public String getOperatorId() {
		return operatorId;
	}

	public void setOperatorId(String operatorId) {
		this.operatorId = operatorId;
	}

	public String getOperatorName() {
		return operatorName;
	}

	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
