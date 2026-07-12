package com.snk.server.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.auth.AccountAuditLogRepository;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenRepository;
import com.snk.server.infrastructure.persistence.auth.RegistrationApprovalTicketRepository;
import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AccountAdministrationServiceTests {

	private final UserRepository users = Mockito.mock(UserRepository.class);
	private final RegistrationApprovalTicketRepository tickets = Mockito.mock(RegistrationApprovalTicketRepository.class);
	private final RefreshTokenRepository refreshTokens = Mockito.mock(RefreshTokenRepository.class);
	private final AccountAuditLogRepository auditLogs = Mockito.mock(AccountAuditLogRepository.class);
	private final PasswordEncoder passwords = Mockito.mock(PasswordEncoder.class);
	private final AccountAdministrationService service = new AccountAdministrationService(
		users, tickets, refreshTokens, auditLogs, passwords
	);

	@Test
	void approvesPendingAccountAndWritesAuditTrail() {
		UserEntity owner = account(AccountRole.OWNER, AccountStatus.ACTIVE);
		UserEntity pending = account(AccountRole.USER, AccountStatus.PENDING);
		when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(owner));
		when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(pending));

		service.approve(1L, 2L);

		assertThat(pending.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(pending.getApprovedByUser()).isSameAs(owner);
		assertThat(pending.getApprovedAt()).isNotNull();
		verify(users).save(pending);
		verify(auditLogs).save(any());
	}

	@Test
	void refusesToDisableLastActiveOwner() {
		UserEntity owner = account(AccountRole.OWNER, AccountStatus.ACTIVE);
		when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(owner));
		when(users.findByRoleAndAccountStatusForUpdate(AccountRole.OWNER, AccountStatus.ACTIVE))
			.thenReturn(List.of(owner));

		assertThatThrownBy(() -> service.disable(1L, 1L))
			.isInstanceOf(ResponseStatusException.class)
			.satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}

	private UserEntity account(AccountRole role, AccountStatus status) {
		UserEntity account = new UserEntity();
		account.setRole(role);
		account.setAccountStatus(status);
		account.setUsername(role.name().toLowerCase());
		return account;
	}
}
