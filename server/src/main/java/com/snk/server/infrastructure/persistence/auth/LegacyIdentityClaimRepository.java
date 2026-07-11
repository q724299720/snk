package com.snk.server.infrastructure.persistence.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyIdentityClaimRepository extends JpaRepository<LegacyIdentityClaimEntity, Long> {

	Optional<LegacyIdentityClaimEntity> findByInstallationId(String installationId);

	Optional<LegacyIdentityClaimEntity> findByAnonymousUser_Id(Long anonymousUserId);

	boolean existsByInstallationId(String installationId);
}
