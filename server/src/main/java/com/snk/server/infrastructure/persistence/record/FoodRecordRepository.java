package com.snk.server.infrastructure.persistence.record;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRecordRepository extends JpaRepository<FoodRecordEntity, Long> {

	List<FoodRecordEntity> findByUser_IdAndDeletedAtIsNullOrderByRecordTimeDesc(Long userId, Pageable pageable);
}
