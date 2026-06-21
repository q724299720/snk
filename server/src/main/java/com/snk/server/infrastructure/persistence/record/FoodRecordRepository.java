package com.snk.server.infrastructure.persistence.record;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FoodRecordRepository extends JpaRepository<FoodRecordEntity, Long> {

	List<FoodRecordEntity> findByUser_IdAndDeletedAtIsNullOrderByRecordTimeDesc(Long userId, Pageable pageable);

	List<FoodRecordEntity> findByIsPublicTrueAndDeletedAtIsNullOrderByRecordTimeDesc(Pageable pageable);

	@Modifying
	@Query("UPDATE FoodRecordEntity record SET record.foodItem = :targetFoodItem WHERE record.foodItem = :sourceFoodItem")
	int reassignFoodItem(FoodItemEntity sourceFoodItem, FoodItemEntity targetFoodItem);
}
