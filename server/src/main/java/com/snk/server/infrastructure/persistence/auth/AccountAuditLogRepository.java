package com.snk.server.infrastructure.persistence.auth;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountAuditLogRepository extends JpaRepository<AccountAuditLogEntity, Long> {

	List<AccountAuditLogEntity> findByTargetUser_IdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);
}
