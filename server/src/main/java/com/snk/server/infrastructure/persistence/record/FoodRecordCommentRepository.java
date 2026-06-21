package com.snk.server.infrastructure.persistence.record;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRecordCommentRepository extends JpaRepository<FoodRecordCommentEntity, Long> {

	List<FoodRecordCommentEntity> findByRecord_IdOrderByCreatedAtDesc(Long recordId, Pageable pageable);
}
