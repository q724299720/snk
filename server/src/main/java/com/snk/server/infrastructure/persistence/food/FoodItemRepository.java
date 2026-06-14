package com.snk.server.infrastructure.persistence.food;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodItemRepository extends JpaRepository<FoodItemEntity, Long> {

	@Query(
		value = """
			SELECT *
			FROM food_items
			WHERE audit_status = 'approved'
			  AND (
			    lower(name) LIKE lower(concat('%', :query, '%'))
			    OR lower(coalesce(alias, '')) LIKE lower(concat('%', :query, '%'))
			    OR lower(coalesce(search_keywords, '')) LIKE lower(concat('%', :query, '%'))
			  )
			ORDER BY
			  CASE
			    WHEN lower(name) = lower(:query) THEN 0
			    WHEN lower(name) LIKE lower(concat(:query, '%')) THEN 1
			    ELSE 2
			  END,
			  similarity(name, :query) DESC,
			  id DESC
			LIMIT 10
			""",
		nativeQuery = true
	)
	List<FoodItemEntity> searchApproved(@Param("query") String query);

	Optional<FoodItemEntity> findByAuditStatusAndBarcode(String auditStatus, String barcode);

	Optional<FoodItemEntity> findFirstByItemTypeAndBarcode(String itemType, String barcode);

	List<FoodItemEntity> findByAuditStatusOrderByCreatedAtDesc(String auditStatus);

	List<FoodItemEntity> findByReportCountGreaterThanEqualOrderByReportCountDescCreatedAtDesc(Integer reportCount);

	List<FoodItemEntity> findByAuditStatusAndCreatedAtBeforeOrderByCreatedAtAsc(String auditStatus, OffsetDateTime createdAt);
}
