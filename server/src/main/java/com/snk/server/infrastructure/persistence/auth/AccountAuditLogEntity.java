package com.snk.server.infrastructure.persistence.auth;

import com.snk.server.infrastructure.persistence.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "account_audit_logs")
public class AccountAuditLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id")
	private UserEntity actorUser;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "target_user_id", nullable = false)
	private UserEntity targetUser;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AccountAuditAction action;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "before_state", columnDefinition = "jsonb")
	private String beforeState;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "after_state", columnDefinition = "jsonb")
	private String afterState;

	@Column(columnDefinition = "TEXT")
	private String note;

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

	public UserEntity getActorUser() {
		return actorUser;
	}

	public void setActorUser(UserEntity actorUser) {
		this.actorUser = actorUser;
	}

	public UserEntity getTargetUser() {
		return targetUser;
	}

	public void setTargetUser(UserEntity targetUser) {
		this.targetUser = targetUser;
	}

	public AccountAuditAction getAction() {
		return action;
	}

	public void setAction(AccountAuditAction action) {
		this.action = action;
	}

	public String getBeforeState() {
		return beforeState;
	}

	public void setBeforeState(String beforeState) {
		this.beforeState = beforeState;
	}

	public String getAfterState() {
		return afterState;
	}

	public void setAfterState(String afterState) {
		this.afterState = afterState;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
