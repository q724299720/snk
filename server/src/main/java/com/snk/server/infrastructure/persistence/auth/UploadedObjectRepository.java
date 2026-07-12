package com.snk.server.infrastructure.persistence.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedObjectRepository extends JpaRepository<UploadedObjectEntity, Long> {
}
