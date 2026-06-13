package com.snk.server.infrastructure.persistence.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByAuthProviderAndAnonymousInstallationId(String authProvider, String anonymousInstallationId);
}
