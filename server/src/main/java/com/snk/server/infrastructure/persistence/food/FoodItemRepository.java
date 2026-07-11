package com.snk.server.infrastructure.persistence.food;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import com.snk.server.infrastructure.persistence.user.UserEntity;

public interface FoodItemRepository extends JpaRepository<FoodItemEntity, Long> {
	@Modifying
	@Query("UPDATE FoodItemEntity food SET food.createdByUser = :target WHERE food.createdByUser = :source")
	int reassignCreator(UserEntity source, UserEntity target);

	long countByCategory(String category);

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
			    OR lower(coalesce(fi.brand, '')) LIKE lower(concat('%', :query, '%'))
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
			  CASE
			    WHEN lower(coalesce(fi.brand, '')) = lower(:query) THEN 0
			    WHEN lower(coalesce(fi.brand, '')) LIKE lower(concat(:query, '%')) THEN 1
			    ELSE 2
			  END,
			  similarity(fi.name, :query) DESC,
			  coalesce(avg(fr.rating), 0) DESC,
			  count(fr.id) DESC,
			  fi.id DESC
			LIMIT 10
			""",
		nativeQuery = true
	)
	List<FoodSearchProjection> searchApproved(@Param("query") String query);

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
			WHERE (
			    fi.audit_status = 'approved'
			    OR (fi.audit_status = 'pending' AND fi.created_by_user_id = :userId)
			  )
			  AND (
			    lower(fi.name) LIKE lower(concat('%', :query, '%'))
			    OR lower(coalesce(fi.alias, '')) LIKE lower(concat('%', :query, '%'))
			    OR lower(coalesce(fi.search_keywords, '')) LIKE lower(concat('%', :query, '%'))
			    OR lower(coalesce(fi.brand, '')) LIKE lower(concat('%', :query, '%'))
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
			  CASE
			    WHEN lower(coalesce(fi.brand, '')) = lower(:query) THEN 0
			    WHEN lower(coalesce(fi.brand, '')) LIKE lower(concat(:query, '%')) THEN 1
			    ELSE 2
			  END,
			  CASE WHEN fi.audit_status = 'pending' THEN 0 ELSE 1 END,
			  similarity(fi.name, :query) DESC,
			  coalesce(avg(fr.rating), 0) DESC,
			  count(fr.id) DESC,
			  fi.id DESC
			LIMIT 10
			""",
		nativeQuery = true
	)
	List<FoodSearchProjection> searchVisibleToUser(@Param("query") String query, @Param("userId") Long userId);

	@Query(
		value = """
			SELECT * FROM food_items
			WHERE audit_status = 'approved'
			  AND lower(regexp_replace(btrim(name), '\\s+', ' ', 'g')) = :normalizedName
			ORDER BY id
			LIMIT 1
			""",
		nativeQuery = true
	)
	Optional<FoodItemEntity> findFirstApprovedByNormalizedName(@Param("normalizedName") String normalizedName);

	@Query(
		value = """
			SELECT * FROM food_items
			WHERE audit_status = 'pending'
			  AND created_by_user_id = :userId
			  AND lower(regexp_replace(btrim(name), '\\s+', ' ', 'g')) = :normalizedName
			ORDER BY id
			LIMIT 1
			""",
		nativeQuery = true
	)
	Optional<FoodItemEntity> findFirstCreatorPendingByNormalizedName(
		@Param("userId") Long userId,
		@Param("normalizedName") String normalizedName
	);

	@Query(
		value = """
			SELECT * FROM food_items
			WHERE id <> :sourceId
			  AND audit_status IN ('approved', 'pending')
			ORDER BY similarity(lower(name), lower(:name)) DESC, id
			LIMIT :limit
			""",
		nativeQuery = true
	)
	List<FoodItemEntity> findMergeCandidates(
		@Param("sourceId") Long sourceId,
		@Param("name") String name,
		@Param("limit") int limit
	);

	Optional<FoodItemEntity> findByAuditStatusAndBarcode(String auditStatus, String barcode);

	Optional<FoodItemEntity> findFirstByItemTypeAndBarcode(String itemType, String barcode);

	List<FoodItemEntity> findByAuditStatusOrderByCreatedAtDesc(String auditStatus);

	List<FoodItemEntity> findByReportCountGreaterThanEqualOrderByReportCountDescCreatedAtDesc(Integer reportCount);

	List<FoodItemEntity> findByAuditStatusAndCreatedAtBeforeOrderByCreatedAtAsc(String auditStatus, OffsetDateTime createdAt);

	long countByAuditStatus(String auditStatus);

	long countByReportCountGreaterThanEqual(Integer reportCount);
}
