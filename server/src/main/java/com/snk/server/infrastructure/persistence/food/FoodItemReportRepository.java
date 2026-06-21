package com.snk.server.infrastructure.persistence.food;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodItemReportRepository extends JpaRepository<FoodItemReportEntity, Long> {

	List<FoodItemReportEntity> findByFoodItem_IdOrderByCreatedAtDesc(Long foodItemId);
}
