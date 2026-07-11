package com.snk.server.domain.auth;

import com.snk.server.infrastructure.persistence.auth.RegistrationApprovalTicketEntity;
import com.snk.server.infrastructure.persistence.auth.RegistrationApprovalTicketRepository;
import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountRegistrationService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final RegistrationApprovalTicketRepository ticketRepository;
	private final PasswordEncoder passwordEncoder;

	public AccountRegistrationService(
		UserRepository userRepository,
		RegistrationApprovalTicketRepository ticketRepository,
		PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.ticketRepository = ticketRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public RegistrationResult register(String username, String password) {
		String displayUsername = normalizeUsername(username);
		PasswordPolicy.validate(password);
		if (userRepository.findByUsernameIgnoreCase(displayUsername).isPresent()) {
			throw usernameConflict();
		}

		UserEntity user = new UserEntity();
		user.setUsername(displayUsername);
		user.setNickname(displayUsername);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setRole(AccountRole.USER);
		user.setAccountStatus(AccountStatus.PENDING);
		user.setAuthProvider("password");
		user.setStatus("active");

		try {
			user = userRepository.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException exception) {
			throw usernameConflict();
		}

		String approvalTicket = newApprovalTicket();
		RegistrationApprovalTicketEntity ticket = new RegistrationApprovalTicketEntity();
		ticket.setUser(user);
		ticket.setTicketHash(SecretHashing.sha256(approvalTicket));
		ticketRepository.save(ticket);

		return new RegistrationResult(
			user.getId(),
			user.getUsername(),
			user.getRole(),
			user.getAccountStatus(),
			approvalTicket
		);
	}

	@Transactional(readOnly = true)
	public RegistrationStatusResult getRegistrationStatus(String approvalTicket) {
		if (approvalTicket == null || approvalTicket.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval ticket is required.");
		}
		RegistrationApprovalTicketEntity ticket = ticketRepository
			.findByTicketHash(SecretHashing.sha256(approvalTicket))
			.filter(value -> value.getTerminalExpiresAt() == null
				|| value.getTerminalExpiresAt().isAfter(OffsetDateTime.now()))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration status is unavailable."));
		return new RegistrationStatusResult(ticket.getUser().getAccountStatus());
	}

	private String normalizeUsername(String username) {
		String normalized = username == null ? "" : username.trim();
		if (!normalized.matches("[\\p{L}\\p{N}._-]{3,64}")) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Username must contain 3-64 letters, numbers, dots, underscores, or hyphens."
			);
		}
		return normalized;
	}

	private String newApprovalTicket() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private ResponseStatusException usernameConflict() {
		return new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered.");
	}
}
