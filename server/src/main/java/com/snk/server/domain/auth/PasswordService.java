package com.snk.server.domain.auth;

import com.snk.server.infrastructure.persistence.auth.AccountAuditAction;
import com.snk.server.infrastructure.persistence.auth.AccountAuditLogEntity;
import com.snk.server.infrastructure.persistence.auth.AccountAuditLogRepository;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenEntity;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AccountAuditLogRepository auditLogRepository;
	private final PasswordEncoder passwordEncoder;

	public PasswordService(
		UserRepository userRepository,
		RefreshTokenRepository refreshTokenRepository,
		AccountAuditLogRepository auditLogRepository,
		PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.auditLogRepository = auditLogRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void changePassword(Long userId, String oldPassword, String newPassword) {
		UserEntity user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
		if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
		}
		PasswordPolicy.validate(newPassword);

		long previousTokenVersion = user.getTokenVersion();
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setMustChangePassword(false);
		user.setTokenVersion(previousTokenVersion + 1);
		userRepository.save(user);

		List<RefreshTokenEntity> sessions = refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(userId);
		OffsetDateTime revokedAt = OffsetDateTime.now();
		sessions.forEach(session -> session.setRevokedAt(revokedAt));
		if (!sessions.isEmpty()) {
			refreshTokenRepository.saveAll(sessions);
		}

		AccountAuditLogEntity audit = new AccountAuditLogEntity();
		audit.setActorUser(user);
		audit.setTargetUser(user);
		audit.setAction(AccountAuditAction.CHANGE_PASSWORD);
		audit.setBeforeState("{\"tokenVersion\":" + previousTokenVersion + "}");
		audit.setAfterState("{\"tokenVersion\":" + user.getTokenVersion() + "}");
		auditLogRepository.save(audit);
	}
}
