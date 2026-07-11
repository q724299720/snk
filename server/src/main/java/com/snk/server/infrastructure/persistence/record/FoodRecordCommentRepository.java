package com.snk.server.infrastructure.persistence.record;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.snk.server.infrastructure.persistence.user.UserEntity;

public interface FoodRecordCommentRepository extends JpaRepository<FoodRecordCommentEntity, Long> {

	List<FoodRecordCommentEntity> findByRecord_IdOrderByCreatedAtDesc(Long recordId, Pageable pageable);

	@Modifying
	@Query("UPDATE FoodRecordCommentEntity comment SET comment.user = :target WHERE comment.user = :source")
	int reassignUser(UserEntity source, UserEntity target);
}
