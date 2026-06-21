package com.snk.server.domain.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordAuditLogEntity;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordAuditLogRepository;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordEntity;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReviewConfigWordServiceTests {

	@Mock
	private ReviewConfigWordRepository reviewConfigWordRepository;

	@Mock
	private ReviewConfigWordAuditLogRepository reviewConfigWordAuditLogRepository;

	private ReviewConfigWordService reviewConfigWordService;

	@BeforeEach
	void setUp() {
		reviewConfigWordService = new ReviewConfigWordService(
			reviewConfigWordRepository,
			reviewConfigWordAuditLogRepository,
			new ObjectMapper().findAndRegisterModules()
		);
	}

	@Test
	void shouldCreateWordAndAppendAuditLog() {
		when(reviewConfigWordRepository.findByWordAndWordType("apple", "valid_food_word"))
			.thenReturn(Optional.empty());
		when(reviewConfigWordRepository.save(any(ReviewConfigWordEntity.class))).thenAnswer(invocation -> saveWord(invocation.getArgument(0), 11L));
		when(reviewConfigWordAuditLogRepository.save(any(ReviewConfigWordAuditLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ReviewConfigWordService.ReviewConfigWordItem item = reviewConfigWordService.createWord(
			new CreateReviewConfigWordCommand(" apple ", "valid_food_word", "manual", "fresh fruit", true, "admin-1", "Admin")
		);

		assertThat(item.id()).isEqualTo(11L);
		assertThat(item.word()).isEqualTo("apple");
		assertThat(item.enabled()).isTrue();
		verify(reviewConfigWordAuditLogRepository).save(any(ReviewConfigWordAuditLogEntity.class));
	}

	@Test
	void shouldRejectDuplicateWordCreation() {
		when(reviewConfigWordRepository.findByWordAndWordType("apple", "valid_food_word"))
			.thenReturn(Optional.of(reviewWord(10L, "apple", "valid_food_word", true)));

		assertThatThrownBy(() -> reviewConfigWordService.createWord(
			new CreateReviewConfigWordCommand("apple", "valid_food_word", "manual", null, true, "admin-1", "Admin")
		)).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void shouldUpdateWordAndAppendAuditLog() {
		ReviewConfigWordEntity existing = reviewWord(12L, "bad", "valid_food_word", true);
		when(reviewConfigWordRepository.findById(12L)).thenReturn(Optional.of(existing));
		when(reviewConfigWordRepository.findByWordAndWordTypeAndIdNot("good", "valid_food_word", 12L))
			.thenReturn(Optional.empty());
		when(reviewConfigWordRepository.save(any(ReviewConfigWordEntity.class))).thenAnswer(invocation -> saveWord(invocation.getArgument(0), 12L));
		when(reviewConfigWordAuditLogRepository.save(any(ReviewConfigWordAuditLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ReviewConfigWordService.ReviewConfigWordItem item = reviewConfigWordService.updateWord(
			new UpdateReviewConfigWordCommand(12L, "good", "valid_food_word", "manual", "normalized", "admin-2", "Moderator")
		);

		assertThat(item.word()).isEqualTo("good");
		assertThat(item.remark()).isEqualTo("normalized");
		verify(reviewConfigWordAuditLogRepository).save(any(ReviewConfigWordAuditLogEntity.class));
	}

	@Test
	void shouldEnableAndDisableWord() {
		ReviewConfigWordEntity existing = reviewWord(13L, "apple", "valid_food_word", false);
		when(reviewConfigWordRepository.findById(13L)).thenReturn(Optional.of(existing));
		when(reviewConfigWordRepository.save(any(ReviewConfigWordEntity.class))).thenAnswer(invocation -> saveWord(invocation.getArgument(0), 13L));
		when(reviewConfigWordAuditLogRepository.save(any(ReviewConfigWordAuditLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ReviewConfigWordService.ReviewConfigWordItem enabled = reviewConfigWordService.enableWord(13L, "admin-3", "Operator");
		ReviewConfigWordService.ReviewConfigWordItem disabled = reviewConfigWordService.disableWord(13L, "admin-3", "Operator");

		assertThat(enabled.enabled()).isTrue();
		assertThat(disabled.enabled()).isFalse();
		verify(reviewConfigWordAuditLogRepository, times(2)).save(any(ReviewConfigWordAuditLogEntity.class));
	}

	@Test
	void shouldListAuditLogs() {
		ReviewConfigWordAuditLogEntity log = new ReviewConfigWordAuditLogEntity();
		log.setReviewConfigWordId(21L);
		log.setActionType("create");
		log.setBeforeValue(null);
		log.setAfterValue("{\"id\":21,\"word\":\"apple\"}");
		log.setOperatorId("admin");
		log.setOperatorName("Admin");
		log.setCreatedAt(OffsetDateTime.parse("2026-06-14T12:00:00Z"));
		when(reviewConfigWordRepository.existsById(21L)).thenReturn(true);
		when(reviewConfigWordAuditLogRepository.findByReviewConfigWordIdOrderByCreatedAtDesc(21L))
			.thenReturn(List.of(log));

		List<ReviewConfigWordService.ReviewConfigWordAuditLogItem> items = reviewConfigWordService.listAuditLogs(21L);

		assertThat(items).hasSize(1);
		assertThat(items.getFirst().actionType()).isEqualTo("create");
		assertThat(items.getFirst().afterValue()).containsEntry("word", "apple");
	}

	@Test
	void shouldRejectAuditLogListForUnknownWord() {
		when(reviewConfigWordRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> reviewConfigWordService.listAuditLogs(99L))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("404 NOT_FOUND");

		verify(reviewConfigWordAuditLogRepository, never()).findByReviewConfigWordIdOrderByCreatedAtDesc(99L);
	}

	private ReviewConfigWordEntity saveWord(ReviewConfigWordEntity entity, Long id) {
		setId(entity, id);
		if (entity.getCreatedAt() == null) {
			entity.setCreatedAt(OffsetDateTime.parse("2026-06-14T12:00:00Z"));
		}
		entity.setUpdatedAt(OffsetDateTime.parse("2026-06-14T12:00:00Z"));
		return entity;
	}

	private ReviewConfigWordEntity reviewWord(Long id, String word, String wordType, boolean enabled) {
		ReviewConfigWordEntity entity = new ReviewConfigWordEntity();
		entity.setWord(word);
		entity.setWordType(wordType);
		entity.setEnabled(enabled);
		entity.setSource("manual");
		entity.setRemark("note");
		entity.setUpdatedBy("admin");
		entity.setCreatedAt(OffsetDateTime.parse("2026-06-14T11:00:00Z"));
		entity.setUpdatedAt(OffsetDateTime.parse("2026-06-14T11:00:00Z"));
		setId(entity, id);
		return entity;
	}

	private void setId(ReviewConfigWordEntity entity, Long id) {
		try {
			var field = ReviewConfigWordEntity.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
