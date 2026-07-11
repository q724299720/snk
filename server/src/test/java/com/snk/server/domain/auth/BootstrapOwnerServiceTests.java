package com.snk.server.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapOwnerServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private AccountAuditLogRepository auditLogRepository;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

	@Test
	void shouldCreateFirstOwnerOnlyWhenNoOwnerExists() throws Exception {
		when(userRepository.existsByRole(AccountRole.OWNER)).thenReturn(false);
		when(userRepository.findByUsernameIgnoreCase("root")).thenReturn(Optional.empty());
		when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		BootstrapOwnerService service = service(new OwnerBootstrapProperties("root", "owner-password-12", false));

		service.run(new DefaultApplicationArguments());

		ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
		verify(userRepository).saveAndFlush(userCaptor.capture());
		assertThat(userCaptor.getValue().getRole()).isEqualTo(AccountRole.OWNER);
		assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(passwordEncoder.matches("owner-password-12", userCaptor.getValue().getPasswordHash())).isTrue();

		ArgumentCaptor<AccountAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(AccountAuditLogEntity.class);
		verify(auditLogRepository).save(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getAction()).isEqualTo(AccountAuditAction.BOOTSTRAP_OWNER);
	}

	@Test
	void shouldNotOverwriteExistingOwnerWithoutForceReset() throws Exception {
		when(userRepository.existsByRole(AccountRole.OWNER)).thenReturn(true);
		BootstrapOwnerService service = service(new OwnerBootstrapProperties("root", "owner-password-12", false));

		service.run(new DefaultApplicationArguments());

		verify(userRepository, never()).saveAndFlush(any(UserEntity.class));
		verify(auditLogRepository, never()).save(any(AccountAuditLogEntity.class));
	}

	@Test
	void forceResetShouldReplaceOwnerPasswordRevokeSessionsAndAudit() throws Exception {
		UserEntity owner = new UserEntity();
		owner.setUsername("root");
		owner.setRole(AccountRole.OWNER);
		owner.setPasswordHash(passwordEncoder.encode("old-owner-pass"));
		owner.setTokenVersion(2);
		RefreshTokenEntity refreshToken = new RefreshTokenEntity();
		when(userRepository.findByUsernameIgnoreCase("root")).thenReturn(Optional.of(owner));
		when(refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(null)).thenReturn(List.of(refreshToken));
		BootstrapOwnerService service = service(new OwnerBootstrapProperties("root", "new-owner-pass-12", true));

		service.run(new DefaultApplicationArguments());

		assertThat(passwordEncoder.matches("new-owner-pass-12", owner.getPasswordHash())).isTrue();
		assertThat(owner.getTokenVersion()).isEqualTo(3);
		assertThat(refreshToken.getRevokedAt()).isNotNull();
		ArgumentCaptor<AccountAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(AccountAuditLogEntity.class);
		verify(auditLogRepository).save(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getAction()).isEqualTo(AccountAuditAction.OWNER_FORCE_RESET);
		assertThat(auditCaptor.getValue().getBeforeState()).doesNotContain("old-owner-pass");
		assertThat(auditCaptor.getValue().getAfterState()).doesNotContain("new-owner-pass-12");
	}

	private BootstrapOwnerService service(OwnerBootstrapProperties properties) {
		return new BootstrapOwnerService(
			properties,
			userRepository,
			refreshTokenRepository,
			auditLogRepository,
			passwordEncoder
		);
	}
}
