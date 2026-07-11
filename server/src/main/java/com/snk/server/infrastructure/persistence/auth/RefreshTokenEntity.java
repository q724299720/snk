package com.snk.server.infrastructure.persistence.auth;

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
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "family_id", nullable = false)
	private UUID familyId;

	@Column(name = "device_id", nullable = false, length = 128)
	private String deviceId;

	@Column(name = "rotated_at")
	private OffsetDateTime rotatedAt;

	@Column(name = "replacement_ciphertext", columnDefinition = "TEXT")
	private String replacementCiphertext;

	@Column(name = "replacement_expires_at")
	private OffsetDateTime replacementExpiresAt;

	@Column(name = "revoked_at")
	private OffsetDateTime revokedAt;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = OffsetDateTime.now();
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

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public UUID getFamilyId() {
		return familyId;
	}

	public void setFamilyId(UUID familyId) {
		this.familyId = familyId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public OffsetDateTime getRotatedAt() {
		return rotatedAt;
	}

	public void setRotatedAt(OffsetDateTime rotatedAt) {
		this.rotatedAt = rotatedAt;
	}

	public String getReplacementCiphertext() {
		return replacementCiphertext;
	}

	public void setReplacementCiphertext(String replacementCiphertext) {
		this.replacementCiphertext = replacementCiphertext;
	}

	public OffsetDateTime getReplacementExpiresAt() {
		return replacementExpiresAt;
	}

	public void setReplacementExpiresAt(OffsetDateTime replacementExpiresAt) {
		this.replacementExpiresAt = replacementExpiresAt;
	}

	public OffsetDateTime getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(OffsetDateTime revokedAt) {
		this.revokedAt = revokedAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
