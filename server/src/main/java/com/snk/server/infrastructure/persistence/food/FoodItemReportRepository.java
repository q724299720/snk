package com.snk.server.infrastructure.persistence.food;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.snk.server.infrastructure.persistence.user.UserEntity;

public interface FoodItemReportRepository extends JpaRepository<FoodItemReportEntity, Long> {

	List<FoodItemReportEntity> findByFoodItem_IdOrderByCreatedAtDesc(Long foodItemId);

	@Modifying
	@Query("UPDATE FoodItemReportEntity report SET report.reporterUser = :target WHERE report.reporterUser = :source")
	int reassignReporter(UserEntity source, UserEntity target);
}
