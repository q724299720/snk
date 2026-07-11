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
@Table(name = "registration_approval_tickets")
public class RegistrationApprovalTicketEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private UserEntity user;

	@Column(name = "ticket_hash", nullable = false, length = 64, unique = true)
	private String ticketHash;

	@Column(name = "terminal_expires_at")
	private OffsetDateTime terminalExpiresAt;

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

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public String getTicketHash() {
		return ticketHash;
	}

	public void setTicketHash(String ticketHash) {
		this.ticketHash = ticketHash;
	}

	public OffsetDateTime getTerminalExpiresAt() {
		return terminalExpiresAt;
	}

	public void setTerminalExpiresAt(OffsetDateTime terminalExpiresAt) {
		this.terminalExpiresAt = terminalExpiresAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
