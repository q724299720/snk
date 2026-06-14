package com.snk.server.infrastructure.persistence.review;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewConfigWordRepository extends JpaRepository<ReviewConfigWordEntity, Long> {

	Optional<ReviewConfigWordEntity> findByWordAndWordType(String word, String wordType);

	Optional<ReviewConfigWordEntity> findByWordAndWordTypeAndIdNot(String word, String wordType, Long id);

	List<ReviewConfigWordEntity> findAllByOrderByUpdatedAtDesc();

	List<ReviewConfigWordEntity> findByEnabledOrderByUpdatedAtDesc(boolean enabled);

	List<ReviewConfigWordEntity> findByWordTypeOrderByUpdatedAtDesc(String wordType);

	List<ReviewConfigWordEntity> findByEnabledAndWordTypeOrderByUpdatedAtDesc(boolean enabled, String wordType);
}
