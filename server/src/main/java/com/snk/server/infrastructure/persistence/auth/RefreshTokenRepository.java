package com.snk.server.infrastructure.persistence.auth;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT token FROM RefreshTokenEntity token WHERE token.tokenHash = :tokenHash")
	Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	List<RefreshTokenEntity> findByFamilyIdAndRevokedAtIsNull(UUID familyId);

	List<RefreshTokenEntity> findByUser_IdAndDeviceIdAndRevokedAtIsNull(Long userId, String deviceId);

	List<RefreshTokenEntity> findByUser_IdAndRevokedAtIsNull(Long userId);
}
