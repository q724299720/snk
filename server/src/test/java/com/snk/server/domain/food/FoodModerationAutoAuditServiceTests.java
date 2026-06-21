package com.snk.server.domain.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.moderation.ModerationProperties;
import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordEntity;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FoodModerationAutoAuditServiceTests {

	@Mock
	private FoodItemRepository foodItemRepository;

	@Mock
	private FoodModerationService foodModerationService;

	@Mock
	private ReviewConfigWordRepository reviewConfigWordRepository;

	private ModerationProperties moderationProperties;
	private Clock clock;
	private FoodModerationAutoAuditService foodModerationAutoAuditService;

	@BeforeEach
	void setUp() {
		moderationProperties = new ModerationProperties();
		moderationProperties.getAutoAudit().setPendingAgeHours(24);
		moderationProperties.getAutoAudit().setRejectKeywords(List.of("test", "junk", "garbage", "unknown"));
		clock = Clock.fixed(Instant.parse("2026-06-14T12:00:00Z"), ZoneOffset.UTC);
		foodModerationAutoAuditService = new FoodModerationAutoAuditService(
			foodItemRepository,
			foodModerationService,
			reviewConfigWordRepository,
			moderationProperties,
			clock
		);
	}

	@Test
	void shouldRejectOnlyObviousGarbagePendingItemsOlderThanCutoff() {
		FoodItemEntity garbageItem = foodItem(1L, "??", "2026-06-13T11:59:59Z");
		FoodItemEntity validItem = foodItem(2L, "Beef Noodles", "2026-06-13T11:59:59Z");
		when(foodItemRepository.findByAuditStatusAndCreatedAtBeforeOrderByCreatedAtAsc(eq("pending"), any(OffsetDateTime.class)))
			.thenReturn(List.of(garbageItem, validItem));
		when(foodModerationService.rejectFoodItem(1L)).thenReturn(moderationItem(1L, "??", "rejected"));

		FoodModerationAutoAuditService.AutoAuditSummary summary = foodModerationAutoAuditService.runAutoAudit();

		ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
		verify(foodItemRepository).findByAuditStatusAndCreatedAtBeforeOrderByCreatedAtAsc(eq("pending"), cutoffCaptor.capture());
		assertThat(cutoffCaptor.getValue()).isEqualTo(OffsetDateTime.parse("2026-06-13T12:00:00Z"));
		assertThat(summary.scannedCount()).isEqualTo(2);
		assertThat(summary.rejectedCount()).isEqualTo(1);
		assertThat(summary.keptPendingCount()).isEqualTo(1);
		assertThat(summary.rejectedFoodItemIds()).containsExactly(1L);
		verify(foodModerationService).rejectFoodItem(1L);
		verify(foodModerationService, never()).rejectFoodItem(2L);
	}

	@Test
	void shouldUseEnabledValidFoodWordsWhenRejectingExtremelyShortGarbage() {
		FoodItemEntity shortGarbage = foodItem(3L, "x9", "2026-06-13T11:59:59Z");
		FoodItemEntity shortValidWord = foodItem(4L, "aa", "2026-06-13T11:59:59Z");
		when(foodItemRepository.findByAuditStatusAndCreatedAtBeforeOrderByCreatedAtAsc(eq("pending"), any(OffsetDateTime.class)))
			.thenReturn(List.of(shortGarbage, shortValidWord));
		when(reviewConfigWordRepository.findByEnabledAndWordTypeOrderByUpdatedAtDesc(true, "valid_food_word"))
			.thenReturn(List.of(reviewWord("aa")));
		when(foodModerationService.rejectFoodItem(3L)).thenReturn(moderationItem(3L, "x9", "rejected"));

		FoodModerationAutoAuditService.AutoAuditSummary summary = foodModerationAutoAuditService.runAutoAudit();

		assertThat(summary.scannedCount()).isEqualTo(2);
		assertThat(summary.rejectedCount()).isEqualTo(1);
		assertThat(summary.rejectedFoodItemIds()).containsExactly(3L);
		verify(foodModerationService).rejectFoodItem(3L);
		verify(foodModerationService, never()).rejectFoodItem(4L);
	}

	@Test
	void shouldSkipAutoAuditWhenDisabled() {
		moderationProperties.getAutoAudit().setEnabled(false);

		FoodModerationAutoAuditService.AutoAuditSummary summary = foodModerationAutoAuditService.runAutoAudit();

		assertThat(summary.scannedCount()).isZero();
		assertThat(summary.rejectedCount()).isZero();
		assertThat(summary.keptPendingCount()).isZero();
		assertThat(summary.rejectedFoodItemIds()).isEmpty();
		verifyNoInteractions(foodItemRepository, foodModerationService);
	}

	@Test
	void shouldSkipDictionaryLoadWhenNoPendingCandidates() {
		when(foodItemRepository.findByAuditStatusAndCreatedAtBeforeOrderByCreatedAtAsc(eq("pending"), any(OffsetDateTime.class)))
			.thenReturn(List.of());

		FoodModerationAutoAuditService.AutoAuditSummary summary = foodModerationAutoAuditService.runAutoAudit();

		assertThat(summary.scannedCount()).isZero();
		assertThat(summary.rejectedCount()).isZero();
		assertThat(summary.keptPendingCount()).isZero();
		assertThat(summary.rejectedFoodItemIds()).isEmpty();
		verifyNoInteractions(reviewConfigWordRepository, foodModerationService);
	}

	private FoodModerationService.FoodModerationItem moderationItem(Long id, String name, String auditStatus) {
		return new FoodModerationService.FoodModerationItem(
			id,
			name,
			"dish",
			"snack",
			null,
			null,
			null,
			"user_generated",
			auditStatus,
			0,
			2L,
			OffsetDateTime.parse("2026-06-13T11:59:59Z"),
			OffsetDateTime.parse("2026-06-13T11:59:59Z")
		);
	}

	private FoodItemEntity foodItem(Long id, String name, String createdAt) {
		FoodItemEntity entity = new FoodItemEntity();
		entity.setName(name);
		entity.setItemType("dish");
		entity.setCategory("snack");
		entity.setSource("user_generated");
		entity.setAuditStatus("pending");
		entity.setSearchKeywords(name);
		entity.setReportCount(0);
		entity.setCreatedAt(OffsetDateTime.parse(createdAt));
		entity.setUpdatedAt(OffsetDateTime.parse(createdAt));
		setId(entity, id);
		return entity;
	}

	private ReviewConfigWordEntity reviewWord(String word) {
		ReviewConfigWordEntity entity = new ReviewConfigWordEntity();
		entity.setWord(word);
		entity.setWordType("valid_food_word");
		entity.setEnabled(true);
		entity.setSource("manual");
		return entity;
	}

	private void setId(FoodItemEntity entity, Long id) {
		try {
			var field = FoodItemEntity.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
