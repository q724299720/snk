package com.snk.server.domain.auth;

import com.snk.server.infrastructure.persistence.auth.AccountAuditAction;
import com.snk.server.infrastructure.persistence.auth.AccountAuditLogEntity;
import com.snk.server.infrastructure.persistence.auth.AccountAuditLogRepository;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenEntity;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenRepository;
import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import com.snk.server.infrastructure.security.OwnerBootstrapProperties;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapOwnerService implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapOwnerService.class);

	private final OwnerBootstrapProperties properties;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AccountAuditLogRepository auditLogRepository;
	private final PasswordEncoder passwordEncoder;

	public BootstrapOwnerService(
		OwnerBootstrapProperties properties,
		UserRepository userRepository,
		RefreshTokenRepository refreshTokenRepository,
		AccountAuditLogRepository auditLogRepository,
		PasswordEncoder passwordEncoder
	) {
		this.properties = properties;
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.auditLogRepository = auditLogRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments arguments) {
		if (properties.username() == null && properties.password() == null && !properties.forceReset()) {
			return;
		}
		if (properties.username() == null || properties.password() == null) {
			throw new IllegalStateException("SNK_OWNER_USERNAME and SNK_OWNER_PASSWORD must be configured together.");
		}
		PasswordPolicy.validate(properties.password());

		if (properties.forceReset()) {
			forceResetOwner();
			return;
		}
		if (userRepository.existsByRole(AccountRole.OWNER)) {
			return;
		}
		if (userRepository.findByUsernameIgnoreCase(properties.username()).isPresent()) {
			throw new IllegalStateException("Bootstrap OWNER username is already used by another account.");
		}
		createFirstOwner();
	}

	private void createFirstOwner() {
		UserEntity owner = new UserEntity();
		owner.setUsername(properties.username().trim());
		owner.setNickname(properties.username().trim());
		owner.setPasswordHash(passwordEncoder.encode(properties.password()));
		owner.setRole(AccountRole.OWNER);
		owner.setAccountStatus(AccountStatus.ACTIVE);
		owner.setAuthProvider("password");
		owner.setStatus("active");
		owner = userRepository.saveAndFlush(owner);
		writeAudit(owner, AccountAuditAction.BOOTSTRAP_OWNER, 0, 0);
		log.info("Account audit action={} userId={} username={}", AccountAuditAction.BOOTSTRAP_OWNER, owner.getId(), owner.getUsername());
	}

	private void forceResetOwner() {
		UserEntity owner = userRepository.findByUsernameIgnoreCase(properties.username().trim())
			.filter(user -> user.getRole() == AccountRole.OWNER)
			.orElseThrow(() -> new IllegalStateException("Configured emergency reset account is not an OWNER."));
		long previousTokenVersion = owner.getTokenVersion();
		owner.setPasswordHash(passwordEncoder.encode(properties.password()));
		owner.setMustChangePassword(false);
		owner.setTokenVersion(previousTokenVersion + 1);
		userRepository.save(owner);

		List<RefreshTokenEntity> sessions = refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(owner.getId());
		OffsetDateTime revokedAt = OffsetDateTime.now();
		sessions.forEach(session -> session.setRevokedAt(revokedAt));
		if (!sessions.isEmpty()) {
			refreshTokenRepository.saveAll(sessions);
		}
		writeAudit(owner, AccountAuditAction.OWNER_FORCE_RESET, previousTokenVersion, owner.getTokenVersion());
		log.info("Account audit action={} userId={} username={}", AccountAuditAction.OWNER_FORCE_RESET, owner.getId(), owner.getUsername());
	}

	private void writeAudit(UserEntity owner, AccountAuditAction action, long beforeVersion, long afterVersion) {
		AccountAuditLogEntity audit = new AccountAuditLogEntity();
		audit.setActorUser(owner);
		audit.setTargetUser(owner);
		audit.setAction(action);
		audit.setBeforeState("{\"tokenVersion\":" + beforeVersion + "}");
		audit.setAfterState("{\"tokenVersion\":" + afterVersion + "}");
		auditLogRepository.save(audit);
	}
}
