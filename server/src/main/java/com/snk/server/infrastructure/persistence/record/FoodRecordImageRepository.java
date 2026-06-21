package com.snk.server.infrastructure.persistence.record;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRecordImageRepository extends JpaRepository<FoodRecordImageEntity, Long> {

	List<FoodRecordImageEntity> findByRecord_IdInOrderByCreatedAtAsc(Collection<Long> recordIds);
}
