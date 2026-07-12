package com.snk.server.domain.auth;

import com.snk.server.infrastructure.persistence.auth.AccountAuditAction;
import com.snk.server.infrastructure.persistence.auth.AccountAuditLogEntity;
import com.snk.server.infrastructure.persistence.auth.AccountAuditLogRepository;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenEntity;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenRepository;
import com.snk.server.infrastructure.persistence.auth.RegistrationApprovalTicketEntity;
import com.snk.server.infrastructure.persistence.auth.RegistrationApprovalTicketRepository;
import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountAdministrationService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private final UserRepository users;
	private final RegistrationApprovalTicketRepository tickets;
	private final RefreshTokenRepository refreshTokens;
	private final AccountAuditLogRepository auditLogs;
	private final PasswordEncoder passwords;

	public AccountAdministrationService(UserRepository users, RegistrationApprovalTicketRepository tickets,
		RefreshTokenRepository refreshTokens, AccountAuditLogRepository auditLogs, PasswordEncoder passwords) {
		this.users = users;
		this.tickets = tickets;
		this.refreshTokens = refreshTokens;
		this.auditLogs = auditLogs;
		this.passwords = passwords;
	}

	@Transactional
	public UserEntity approve(Long actorId, Long targetId) {
		UserEntity actor = activeOwner(actorId);
		UserEntity target = target(targetId);
		if (target.getAccountStatus() != AccountStatus.PENDING) throw conflict("Only pending accounts can be approved.");
		AccountStatus before = target.getAccountStatus();
		target.setAccountStatus(AccountStatus.ACTIVE);
		target.setApprovedByUser(actor);
		target.setApprovedAt(OffsetDateTime.now());
		users.save(target);
		completeTicket(target);
		audit(actor, target, AccountAuditAction.APPROVE, before, target.getAccountStatus());
		return target;
	}

	@Transactional
	public UserEntity reject(Long actorId, Long targetId) {
		UserEntity actor = activeOwner(actorId);
		UserEntity target = target(targetId);
		if (target.getAccountStatus() != AccountStatus.PENDING) throw conflict("Only pending accounts can be rejected.");
		AccountStatus before = target.getAccountStatus();
		target.setAccountStatus(AccountStatus.REJECTED);
		invalidate(target);
		users.save(target);
		completeTicket(target);
		audit(actor, target, AccountAuditAction.REJECT, before, target.getAccountStatus());
		return target;
	}

	@Transactional
	public UserEntity disable(Long actorId, Long targetId) {
		UserEntity actor = activeOwner(actorId);
		UserEntity target = target(targetId);
		if (target.getAccountStatus() != AccountStatus.ACTIVE) throw conflict("Only active accounts can be disabled.");
		ensureOwnerMayLoseActiveStatus(actor, target);
		AccountStatus before = target.getAccountStatus();
		target.setAccountStatus(AccountStatus.DISABLED);
		invalidate(target);
		users.save(target);
		audit(actor, target, AccountAuditAction.DISABLE, before, target.getAccountStatus());
		return target;
	}

	@Transactional
	public UserEntity enable(Long actorId, Long targetId) {
		UserEntity actor = activeOwner(actorId);
		UserEntity target = target(targetId);
		if (target.getAccountStatus() != AccountStatus.DISABLED) throw conflict("Only disabled accounts can be enabled.");
		AccountStatus before = target.getAccountStatus();
		target.setAccountStatus(AccountStatus.ACTIVE);
		invalidate(target);
		users.save(target);
		audit(actor, target, AccountAuditAction.ENABLE, before, target.getAccountStatus());
		return target;
	}

	@Transactional
	public UserEntity promote(Long actorId, Long targetId) {
		UserEntity actor = activeOwner(actorId);
		UserEntity target = target(targetId);
		if (target.getAccountStatus() != AccountStatus.ACTIVE || target.getRole() == AccountRole.OWNER) throw conflict("Only active USER accounts can be promoted.");
		AccountRole before = target.getRole();
		target.setRole(AccountRole.OWNER);
		invalidate(target);
		users.save(target);
		audit(actor, target, AccountAuditAction.PROMOTE, before, target.getRole());
		return target;
	}

	@Transactional
	public UserEntity demote(Long actorId, Long targetId) {
		UserEntity actor = activeOwner(actorId);
		UserEntity target = target(targetId);
		if (actor == target || actorId.equals(targetId)) throw conflict("An OWNER cannot demote itself.");
		if (target.getRole() != AccountRole.OWNER) throw conflict("Only OWNER accounts can be demoted.");
		ensureOwnerMayLoseRole(target);
		AccountRole before = target.getRole();
		target.setRole(AccountRole.USER);
		invalidate(target);
		users.save(target);
		audit(actor, target, AccountAuditAction.DEMOTE, before, target.getRole());
		return target;
	}

	@Transactional
	public void revokeSessions(Long actorId, Long targetId) {
		UserEntity actor = activeOwner(actorId);
		UserEntity target = target(targetId);
		invalidate(target);
		users.save(target);
		audit(actor, target, AccountAuditAction.REVOKE_SESSIONS, target.getTokenVersion() - 1, target.getTokenVersion());
	}

	@Transactional
	public String resetPassword(Long actorId, Long targetId) {
		UserEntity actor = activeOwner(actorId);
		UserEntity target = target(targetId);
		String temporaryPassword = randomTemporaryPassword();
		target.setPasswordHash(passwords.encode(temporaryPassword));
		target.setMustChangePassword(true);
		invalidate(target);
		users.save(target);
		audit(actor, target, AccountAuditAction.RESET_PASSWORD, null, target.getTokenVersion());
		return temporaryPassword;
	}

	@Transactional(readOnly = true)
	public List<UserEntity> listAccounts() { return users.findAll(); }

	private UserEntity activeOwner(Long userId) {
		UserEntity actor = target(userId);
		if (actor.getRole() != AccountRole.OWNER || actor.getAccountStatus() != AccountStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "OWNER account is required.");
		return actor;
	}

	private UserEntity target(Long userId) {
		return users.findByIdForUpdate(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
	}

	private void ensureOwnerMayLoseActiveStatus(UserEntity actor, UserEntity target) {
		if (target.getRole() == AccountRole.OWNER) ensureOwnerMayLoseRole(target);
	}

	private void ensureOwnerMayLoseRole(UserEntity target) {
		List<UserEntity> activeOwners = users.findByRoleAndAccountStatusForUpdate(AccountRole.OWNER, AccountStatus.ACTIVE);
		if (activeOwners.size() <= 1) throw conflict("The last active OWNER cannot be changed.");
	}

	private void invalidate(UserEntity target) {
		target.setTokenVersion(target.getTokenVersion() + 1);
		List<RefreshTokenEntity> sessions = refreshTokens.findByUser_IdAndRevokedAtIsNull(target.getId());
		OffsetDateTime now = OffsetDateTime.now();
		sessions.forEach(session -> session.setRevokedAt(now));
		if (!sessions.isEmpty()) refreshTokens.saveAll(sessions);
	}

	private void completeTicket(UserEntity target) {
		tickets.findByUser_Id(target.getId()).ifPresent(ticket -> {
			ticket.setTerminalExpiresAt(OffsetDateTime.now().plusDays(30));
			tickets.save(ticket);
		});
	}

	private void audit(UserEntity actor, UserEntity target, AccountAuditAction action, Object before, Object after) {
		AccountAuditLogEntity audit = new AccountAuditLogEntity();
		audit.setActorUser(actor);
		audit.setTargetUser(target);
		audit.setAction(action);
		audit.setBeforeState(state(before));
		audit.setAfterState(state(after));
		auditLogs.save(audit);
	}

	private static String state(Object value) { return value == null ? null : "{\"value\":\"" + value + "\"}"; }
	private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
	private static String randomTemporaryPassword() { byte[] bytes = new byte[18]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
}
