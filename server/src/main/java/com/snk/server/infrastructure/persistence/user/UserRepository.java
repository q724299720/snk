package com.snk.server.infrastructure.persistence.user;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByAuthProviderAndAnonymousInstallationId(String authProvider, String anonymousInstallationId);

	Optional<UserEntity> findByAnonymousInstallationId(String anonymousInstallationId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT user FROM UserEntity user WHERE user.anonymousInstallationId = :installationId")
	Optional<UserEntity> findByAnonymousInstallationIdForUpdate(@Param("installationId") String installationId);

	Optional<UserEntity> findByUsernameIgnoreCase(String username);

	long countByRoleAndAccountStatus(AccountRole role, AccountStatus accountStatus);

	boolean existsByRole(AccountRole role);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		SELECT account
		FROM UserEntity account
		WHERE account.role = :role
		  AND account.accountStatus = :accountStatus
		ORDER BY account.id
		""")
	List<UserEntity> findByRoleAndAccountStatusForUpdate(
		@Param("role") AccountRole role,
		@Param("accountStatus") AccountStatus accountStatus
	);
}
