package com.snk.server.infrastructure.persistence.recognition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecognitionTaskRepository extends JpaRepository<RecognitionTaskEntity, Long> {

	Page<RecognitionTaskEntity> findByOrderByCreatedAtDesc(Pageable pageable);

	Page<RecognitionTaskEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

	Page<RecognitionTaskEntity> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	Page<RecognitionTaskEntity> findByStatusAndUser_IdOrderByCreatedAtDesc(String status, Long userId, Pageable pageable);

	long countByStatus(String status);
}
