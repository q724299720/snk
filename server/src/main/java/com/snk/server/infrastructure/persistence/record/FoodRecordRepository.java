package com.snk.server.infrastructure.persistence.record;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRecordRepository extends JpaRepository<FoodRecordEntity, Long> {
}
