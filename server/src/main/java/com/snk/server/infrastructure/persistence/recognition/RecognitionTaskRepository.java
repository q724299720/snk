package com.snk.server.infrastructure.persistence.recognition;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecognitionTaskRepository extends JpaRepository<RecognitionTaskEntity, Long> {
}
