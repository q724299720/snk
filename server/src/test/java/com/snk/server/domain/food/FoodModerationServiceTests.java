package com.snk.server.domain.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FoodModerationServiceTests {

	@Mock
	private FoodItemRepository foodItemRepository;

	@InjectMocks
	private FoodModerationService foodModerationService;

	@Test
	void shouldListPendingItemsInNewestFirstOrder() {
		when(foodItemRepository.findByAuditStatusOrderByCreatedAtDesc("pending"))
			.thenReturn(List.of(foodItem(2L, "Second Pending", 0), foodItem(1L, "First Pending", 0)));

		List<FoodModerationService.FoodModerationItem> items = foodModerationService.listPendingItems();

		assertThat(items).hasSize(2);
		assertThat(items.getFirst().name()).isEqualTo("Second Pending");
		assertThat(items.getFirst().auditStatus()).isEqualTo("pending");
	}

	@Test
	void shouldNormalizeReportedItemsLowerBound() {
		when(foodItemRepository.findByReportCountGreaterThanEqualOrderByReportCountDescCreatedAtDesc(1))
			.thenReturn(List.of(foodItem(3L, "Reported Item", 4)));

		List<FoodModerationService.FoodModerationItem> items = foodModerationService.listReportedItems(0);

		assertThat(items).hasSize(1);
		assertThat(items.getFirst().reportCount()).isEqualTo(4);
	}

	private FoodItemEntity foodItem(Long id, String name, int reportCount) {
		FoodItemEntity entity = new FoodItemEntity();
		entity.setName(name);
		entity.setItemType("dish");
		entity.setCategory("snack");
		entity.setSource("user_generated");
		entity.setAuditStatus("pending");
		entity.setSearchKeywords(name);
		entity.setReportCount(reportCount);
		entity.setCreatedAt(OffsetDateTime.parse("2026-06-14T12:00:00Z"));
		entity.setUpdatedAt(OffsetDateTime.parse("2026-06-14T12:00:00Z"));
		setId(entity, id);
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
