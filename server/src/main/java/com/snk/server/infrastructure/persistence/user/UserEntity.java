package com.snk.server.infrastructure.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 64)
	private String nickname;

	@Column(length = 512)
	private String avatar;

	@Column(length = 32)
	private String phone;

	@Column(length = 255)
	private String email;

	@Column(length = 64)
	private String username;

	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(length = 16)
	private AccountRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_status", length = 16)
	private AccountStatus accountStatus;

	@Column(name = "token_version", nullable = false)
	private long tokenVersion;

	@Column(name = "must_change_password", nullable = false)
	private boolean mustChangePassword;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_by_user_id")
	private UserEntity approvedByUser;

	@Column(name = "approved_at")
	private OffsetDateTime approvedAt;

	@Column(name = "last_login_at")
	private OffsetDateTime lastLoginAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "claimed_by_user_id")
	private UserEntity claimedByUser;

	@Column(name = "claimed_at")
	private OffsetDateTime claimedAt;

	@Column(name = "auth_provider", nullable = false, length = 32)
	private String authProvider;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(name = "anonymous_installation_id", length = 128)
	private String anonymousInstallationId;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "last_seen_at", nullable = false)
	private OffsetDateTime lastSeenAt;

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (lastSeenAt == null) {
			lastSeenAt = now;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public AccountRole getRole() {
		return role;
	}

	public void setRole(AccountRole role) {
		this.role = role;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

	public long getTokenVersion() {
		return tokenVersion;
	}

	public void setTokenVersion(long tokenVersion) {
		this.tokenVersion = tokenVersion;
	}

	public boolean isMustChangePassword() {
		return mustChangePassword;
	}

	public void setMustChangePassword(boolean mustChangePassword) {
		this.mustChangePassword = mustChangePassword;
	}

	public UserEntity getApprovedByUser() {
		return approvedByUser;
	}

	public void setApprovedByUser(UserEntity approvedByUser) {
		this.approvedByUser = approvedByUser;
	}

	public OffsetDateTime getApprovedAt() {
		return approvedAt;
	}

	public void setApprovedAt(OffsetDateTime approvedAt) {
		this.approvedAt = approvedAt;
	}

	public OffsetDateTime getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(OffsetDateTime lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	public UserEntity getClaimedByUser() {
		return claimedByUser;
	}

	public void setClaimedByUser(UserEntity claimedByUser) {
		this.claimedByUser = claimedByUser;
	}

	public OffsetDateTime getClaimedAt() {
		return claimedAt;
	}

	public void setClaimedAt(OffsetDateTime claimedAt) {
		this.claimedAt = claimedAt;
	}

	public String getAuthProvider() {
		return authProvider;
	}

	public void setAuthProvider(String authProvider) {
		this.authProvider = authProvider;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAnonymousInstallationId() {
		return anonymousInstallationId;
	}

	public void setAnonymousInstallationId(String anonymousInstallationId) {
		this.anonymousInstallationId = anonymousInstallationId;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public OffsetDateTime getLastSeenAt() {
		return lastSeenAt;
	}

	public void setLastSeenAt(OffsetDateTime lastSeenAt) {
		this.lastSeenAt = lastSeenAt;
	}
}
