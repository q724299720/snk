package com.snk.server.domain.review;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordAuditLogEntity;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordAuditLogRepository;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordEntity;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewConfigWordService {

	private static final TypeReference<LinkedHashMap<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {
	};

	private final ReviewConfigWordRepository reviewConfigWordRepository;
	private final ReviewConfigWordAuditLogRepository reviewConfigWordAuditLogRepository;
	private final ObjectMapper objectMapper;

	public ReviewConfigWordService(
		ReviewConfigWordRepository reviewConfigWordRepository,
		ReviewConfigWordAuditLogRepository reviewConfigWordAuditLogRepository,
		ObjectMapper objectMapper
	) {
		this.reviewConfigWordRepository = reviewConfigWordRepository;
		this.reviewConfigWordAuditLogRepository = reviewConfigWordAuditLogRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public List<ReviewConfigWordItem> listWords(Boolean enabled, String wordType) {
		String normalizedWordType = normalizeOptional(wordType);
		List<ReviewConfigWordEntity> entities;
		if (enabled == null && normalizedWordType == null) {
			entities = reviewConfigWordRepository.findAllByOrderByUpdatedAtDesc();
		} else if (enabled != null && normalizedWordType == null) {
			entities = reviewConfigWordRepository.findByEnabledOrderByUpdatedAtDesc(enabled);
		} else if (enabled == null) {
			entities = reviewConfigWordRepository.findByWordTypeOrderByUpdatedAtDesc(normalizedWordType);
		} else {
			entities = reviewConfigWordRepository.findByEnabledAndWordTypeOrderByUpdatedAtDesc(enabled, normalizedWordType);
		}
		return entities.stream().map(this::toItem).toList();
	}

	@Transactional(readOnly = true)
	public List<ReviewConfigWordAuditLogItem> listAuditLogs(Long reviewConfigWordId) {
		if (!reviewConfigWordRepository.existsById(reviewConfigWordId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review config word not found.");
		}
		return reviewConfigWordAuditLogRepository.findByReviewConfigWordIdOrderByCreatedAtDesc(reviewConfigWordId)
			.stream()
			.map(this::toAuditLogItem)
			.toList();
	}

	@Transactional
	public ReviewConfigWordItem createWord(CreateReviewConfigWordCommand command) {
		String word = normalizeRequired(command.word(), "word");
		String wordType = normalizeRequired(command.wordType(), "wordType");
		if (reviewConfigWordRepository.findByWordAndWordType(word, wordType).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "word and wordType already exist");
		}

		ReviewConfigWordEntity entity = new ReviewConfigWordEntity();
		entity.setWord(word);
		entity.setWordType(wordType);
		entity.setEnabled(command.enabled() == null || command.enabled());
		entity.setSource(normalizeRequired(command.source(), "source"));
		entity.setRemark(normalizeOptional(command.remark()));
		entity.setUpdatedBy(normalizeRequired(command.operatorId(), "operatorId"));

		ReviewConfigWordEntity saved = reviewConfigWordRepository.save(entity);
		appendAuditLog("create", null, snapshot(saved), command.operatorId(), command.operatorName(), saved.getId());
		return toItem(saved);
	}

	@Transactional
	public ReviewConfigWordItem updateWord(UpdateReviewConfigWordCommand command) {
		ReviewConfigWordEntity entity = reviewConfigWordRepository.findById(command.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review config word not found."));

		String word = normalizeRequired(command.word(), "word");
		String wordType = normalizeRequired(command.wordType(), "wordType");
		reviewConfigWordRepository.findByWordAndWordTypeAndIdNot(word, wordType, entity.getId())
			.ifPresent(duplicate -> {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "word and wordType already exist");
			});

		Map<String, Object> beforeValue = snapshot(entity);
		entity.setWord(word);
		entity.setWordType(wordType);
		entity.setSource(normalizeRequired(command.source(), "source"));
		entity.setRemark(normalizeOptional(command.remark()));
		entity.setUpdatedBy(normalizeRequired(command.operatorId(), "operatorId"));
		ReviewConfigWordEntity saved = reviewConfigWordRepository.save(entity);
		appendAuditLog("update", beforeValue, snapshot(saved), command.operatorId(), command.operatorName(), saved.getId());
		return toItem(saved);
	}

	@Transactional
	public ReviewConfigWordItem enableWord(Long id, String operatorId, String operatorName) {
		return setEnabledState(id, true, "enable", operatorId, operatorName);
	}

	@Transactional
	public ReviewConfigWordItem disableWord(Long id, String operatorId, String operatorName) {
		return setEnabledState(id, false, "disable", operatorId, operatorName);
	}

	private ReviewConfigWordItem setEnabledState(
		Long id,
		boolean enabled,
		String actionType,
		String operatorId,
		String operatorName
	) {
		ReviewConfigWordEntity entity = reviewConfigWordRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review config word not found."));
		Map<String, Object> beforeValue = snapshot(entity);
		entity.setEnabled(enabled);
		entity.setUpdatedBy(normalizeRequired(operatorId, "operatorId"));
		ReviewConfigWordEntity saved = reviewConfigWordRepository.save(entity);
		appendAuditLog(actionType, beforeValue, snapshot(saved), operatorId, operatorName, saved.getId());
		return toItem(saved);
	}

	private void appendAuditLog(
		String actionType,
		Map<String, Object> beforeValue,
		Map<String, Object> afterValue,
		String operatorId,
		String operatorName,
		Long wordId
	) {
		ReviewConfigWordAuditLogEntity log = new ReviewConfigWordAuditLogEntity();
		log.setReviewConfigWordId(wordId);
		log.setActionType(actionType);
		log.setBeforeValue(writeJson(beforeValue));
		log.setAfterValue(writeJson(afterValue));
		log.setOperatorId(normalizeOptional(operatorId));
		log.setOperatorName(normalizeOptional(operatorName));
		log.setCreatedAt(OffsetDateTime.now());
		reviewConfigWordAuditLogRepository.save(log);
	}

	private ReviewConfigWordItem toItem(ReviewConfigWordEntity entity) {
		return new ReviewConfigWordItem(
			entity.getId(),
			entity.getWord(),
			entity.getWordType(),
			entity.isEnabled(),
			entity.getSource(),
			entity.getRemark(),
			entity.getUpdatedBy(),
			entity.getCreatedAt(),
			entity.getUpdatedAt()
		);
	}

	private ReviewConfigWordAuditLogItem toAuditLogItem(ReviewConfigWordAuditLogEntity entity) {
		return new ReviewConfigWordAuditLogItem(
			entity.getId(),
			entity.getReviewConfigWordId(),
			entity.getActionType(),
			readJson(entity.getBeforeValue()),
			readJson(entity.getAfterValue()),
			entity.getOperatorId(),
			entity.getOperatorName(),
			entity.getCreatedAt()
		);
	}

	private Map<String, Object> snapshot(ReviewConfigWordEntity entity) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("id", entity.getId());
		snapshot.put("word", entity.getWord());
		snapshot.put("wordType", entity.getWordType());
		snapshot.put("enabled", entity.isEnabled());
		snapshot.put("source", entity.getSource());
		snapshot.put("remark", entity.getRemark());
		snapshot.put("updatedBy", entity.getUpdatedBy());
		snapshot.put("createdAt", entity.getCreatedAt());
		snapshot.put("updatedAt", entity.getUpdatedAt());
		return snapshot;
	}

	private String writeJson(Map<String, Object> value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize snapshot.", exception);
		}
	}

	private Map<String, Object> readJson(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(value, SNAPSHOT_TYPE);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to deserialize snapshot.", exception);
		}
	}

	private String normalizeRequired(String value, String fieldName) {
		String normalized = normalizeOptional(value);
		if (normalized == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must not be blank");
		}
		return normalized;
	}

	private String normalizeOptional(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim().replaceAll("\\s+", " ");
		return normalized.isBlank() ? null : normalized;
	}

	public record ReviewConfigWordItem(
		Long id,
		String word,
		String wordType,
		boolean enabled,
		String source,
		String remark,
		String updatedBy,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
	) {
	}

	public record ReviewConfigWordAuditLogItem(
		Long id,
		Long reviewConfigWordId,
		String actionType,
		Map<String, Object> beforeValue,
		Map<String, Object> afterValue,
		String operatorId,
		String operatorName,
		OffsetDateTime createdAt
	) {
	}
}
