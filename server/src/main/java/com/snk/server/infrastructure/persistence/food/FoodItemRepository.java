package com.snk.server.infrastructure.persistence.food;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodItemRepository extends JpaRepository<FoodItemEntity, Long> {

	@Query(
		value = """
			SELECT
			  fi.id AS id,
			  fi.name AS name,
			  fi.item_type AS itemType,
			  fi.category AS category,
			  fi.subcategory AS subcategory,
			  fi.brand AS brand,
			  fi.barcode AS barcode,
			  fi.cover_image_url AS coverImageUrl,
			  fi.audit_status AS auditStatus,
			  round(avg(fr.rating)::numeric, 1) AS averageRating
			FROM food_items fi
			LEFT JOIN food_records fr
			  ON fr.food_item_id = fi.id
			 AND fr.deleted_at IS NULL
			WHERE fi.audit_status = 'approved'
			  AND (
			    lower(fi.name) LIKE lower(concat('%', :query, '%'))
			    OR lower(coalesce(fi.alias, '')) LIKE lower(concat('%', :query, '%'))
			    OR lower(coalesce(fi.search_keywords, '')) LIKE lower(concat('%', :query, '%'))
			  )
			GROUP BY
			  fi.id,
			  fi.name,
			  fi.item_type,
			  fi.category,
			  fi.subcategory,
			  fi.brand,
			  fi.barcode,
			  fi.cover_image_url,
			  fi.audit_status
			ORDER BY
			  CASE
			    WHEN lower(fi.name) = lower(:query) THEN 0
			    WHEN lower(fi.name) LIKE lower(concat(:query, '%')) THEN 1
			    ELSE 2
			  END,
			  similarity(fi.name, :query) DESC,
			  coalesce(avg(fr.rating), 0) DESC,
			  fi.id DESC
			LIMIT 10
			""",
		nativeQuery = true
	)
	List<FoodSearchProjection> searchApproved(@Param("query") String query);

	Optional<FoodItemEntity> findByAuditStatusAndBarcode(String auditStatus, String barcode);

	Optional<FoodItemEntity> findFirstByItemTypeAndBarcode(String itemType, String barcode);

	List<FoodItemEntity> findByAuditStatusOrderByCreatedAtDesc(String auditStatus);

	List<FoodItemEntity> findByReportCountGreaterThanEqualOrderByReportCountDescCreatedAtDesc(Integer reportCount);

	List<FoodItemEntity> findByAuditStatusAndCreatedAtBeforeOrderByCreatedAtAsc(String auditStatus, OffsetDateTime createdAt);

	long countByAuditStatus(String auditStatus);

	long countByReportCountGreaterThanEqual(Integer reportCount);
}
