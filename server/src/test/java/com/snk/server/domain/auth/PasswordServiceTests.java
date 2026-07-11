package com.snk.server.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.auth.AccountAuditAction;
import com.snk.server.infrastructure.persistence.auth.AccountAuditLogEntity;
import com.snk.server.infrastructure.persistence.auth.AccountAuditLogRepository;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenEntity;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private AccountAuditLogRepository auditLogRepository;

	private BCryptPasswordEncoder passwordEncoder;
	private PasswordService passwordService;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder(12);
		passwordService = new PasswordService(
			userRepository,
			refreshTokenRepository,
			auditLogRepository,
			passwordEncoder
		);
	}

	@Test
	void changePasswordShouldRevokeSessionsAndIncrementTokenVersion() {
		UserEntity user = new UserEntity();
		user.setUsername("alice");
		user.setPasswordHash(passwordEncoder.encode("old-password-12"));
		user.setTokenVersion(4);
		user.setMustChangePassword(true);
		RefreshTokenEntity first = new RefreshTokenEntity();
		RefreshTokenEntity second = new RefreshTokenEntity();
		when(userRepository.findById(7L)).thenReturn(Optional.of(user));
		when(refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(7L)).thenReturn(List.of(first, second));

		passwordService.changePassword(7L, "old-password-12", "new-password-12");

		assertThat(passwordEncoder.matches("new-password-12", user.getPasswordHash())).isTrue();
		assertThat(user.getTokenVersion()).isEqualTo(5);
		assertThat(user.isMustChangePassword()).isFalse();
		assertThat(first.getRevokedAt()).isNotNull();
		assertThat(second.getRevokedAt()).isNotNull();
		verify(refreshTokenRepository).saveAll(List.of(first, second));

		ArgumentCaptor<AccountAuditLogEntity> auditCaptor = ArgumentCaptor.forClass(AccountAuditLogEntity.class);
		verify(auditLogRepository).save(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getAction()).isEqualTo(AccountAuditAction.CHANGE_PASSWORD);
		assertThat(auditCaptor.getValue().getBeforeState()).doesNotContain("old-password-12");
		assertThat(auditCaptor.getValue().getAfterState()).doesNotContain("new-password-12");
	}

	@Test
	void changePasswordShouldRejectWrongOldPassword() {
		UserEntity user = new UserEntity();
		user.setPasswordHash(passwordEncoder.encode("old-password-12"));
		when(userRepository.findById(7L)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> passwordService.changePassword(7L, "wrong-password", "new-password-12"))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
			.isEqualTo(400);
	}
}
