package com.snk.server.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnonymousAuthServiceTests {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private AnonymousAuthService anonymousAuthService;

	@Test
	void shouldCreateAnonymousUserWhenInstallationIdIsNew() {
		when(userRepository.findByAuthProviderAndAnonymousInstallationId("anonymous", "install-1"))
			.thenReturn(Optional.empty());
		when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
			UserEntity entity = invocation.getArgument(0);
			entity.setCreatedAt(OffsetDateTime.now());
			entity.setUpdatedAt(OffsetDateTime.now());
			entity.setLastSeenAt(OffsetDateTime.now());
			return entity;
		});

		AnonymousUserResult result = anonymousAuthService.initializeAnonymousUser("install-1");

		ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
		verify(userRepository).save(captor.capture());
		UserEntity saved = captor.getValue();
		assertThat(saved.getAuthProvider()).isEqualTo("anonymous");
		assertThat(saved.getStatus()).isEqualTo("active");
		assertThat(saved.getAnonymousInstallationId()).isEqualTo("install-1");
		assertThat(result.newlyCreated()).isTrue();
		assertThat(result.installationId()).isEqualTo("install-1");
	}

	@Test
	void shouldReuseAnonymousUserWhenInstallationIdExists() {
		UserEntity existing = new UserEntity();
		existing.setAuthProvider("anonymous");
		existing.setStatus("active");
		existing.setAnonymousInstallationId("install-2");
		existing.setCreatedAt(OffsetDateTime.now().minusDays(1));
		existing.setUpdatedAt(OffsetDateTime.now().minusDays(1));
		existing.setLastSeenAt(OffsetDateTime.now().minusDays(1));

		when(userRepository.findByAuthProviderAndAnonymousInstallationId("anonymous", "install-2"))
			.thenReturn(Optional.of(existing));
		when(userRepository.save(existing)).thenReturn(existing);

		AnonymousUserResult result = anonymousAuthService.initializeAnonymousUser("install-2");

		verify(userRepository).save(existing);
		assertThat(result.newlyCreated()).isFalse();
		assertThat(result.installationId()).isEqualTo("install-2");
		assertThat(result.authProvider()).isEqualTo("anonymous");
	}
}
