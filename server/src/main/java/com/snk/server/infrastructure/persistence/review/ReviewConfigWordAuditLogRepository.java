package com.snk.server.infrastructure.persistence.review;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewConfigWordAuditLogRepository extends JpaRepository<ReviewConfigWordAuditLogEntity, Long> {

	List<ReviewConfigWordAuditLogEntity> findByReviewConfigWordIdOrderByCreatedAtDesc(Long reviewConfigWordId);
}
