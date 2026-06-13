package com.snk.server.domain.user;

import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnonymousAuthService {

	private final UserRepository userRepository;

	public AnonymousAuthService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public AnonymousUserResult initializeAnonymousUser(String installationId) {
		return userRepository
			.findByAuthProviderAndAnonymousInstallationId(AuthProvider.ANONYMOUS.getValue(), installationId)
			.map(existingUser -> {
				existingUser.setLastSeenAt(OffsetDateTime.now());
				UserEntity savedUser = userRepository.save(existingUser);
				return toResult(savedUser, false);
			})
			.orElseGet(() -> {
				UserEntity user = new UserEntity();
				user.setAuthProvider(AuthProvider.ANONYMOUS.getValue());
				user.setStatus(UserStatus.ACTIVE.getValue());
				user.setAnonymousInstallationId(installationId);
				UserEntity savedUser = userRepository.save(user);
				return toResult(savedUser, true);
			});
	}

	private AnonymousUserResult toResult(UserEntity user, boolean newlyCreated) {
		return new AnonymousUserResult(
			user.getId(),
			user.getAuthProvider(),
			user.getAnonymousInstallationId(),
			newlyCreated,
			user.getCreatedAt(),
			user.getLastSeenAt()
		);
	}
}
