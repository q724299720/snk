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
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "legacy_identity_claims")
public class LegacyIdentityClaimEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "installation_id", nullable = false, length = 128, unique = true)
	private String installationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "anonymous_user_id", nullable = false, unique = true)
	private UserEntity anonymousUser;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "claimed_by_user_id", nullable = false)
	private UserEntity claimedByUser;

	@Column(name = "claimed_at", nullable = false)
	private OffsetDateTime claimedAt;

	@PrePersist
	void onCreate() {
		if (claimedAt == null) {
			claimedAt = OffsetDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public String getInstallationId() {
		return installationId;
	}

	public void setInstallationId(String installationId) {
		this.installationId = installationId;
	}

	public UserEntity getAnonymousUser() {
		return anonymousUser;
	}

	public void setAnonymousUser(UserEntity anonymousUser) {
		this.anonymousUser = anonymousUser;
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
}
