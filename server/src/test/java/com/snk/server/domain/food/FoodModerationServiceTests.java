package com.snk.server.domain.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class FoodModerationServiceTests {

	@Mock
	private FoodItemRepository foodItemRepository;

	@Mock
	private FoodRecordRepository foodRecordRepository;

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

	@Test
	void shouldApproveFoodItem() {
		FoodItemEntity entity = foodItem(4L, "Approval Target", 2);
		when(foodItemRepository.findById(4L)).thenReturn(java.util.Optional.of(entity));
		when(foodItemRepository.save(any(FoodItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		FoodModerationService.FoodModerationItem item = foodModerationService.approveFoodItem(4L);

		assertThat(item.auditStatus()).isEqualTo("approved");
		assertThat(entity.getAuditStatus()).isEqualTo("approved");
	}

	@Test
	void shouldRejectFoodItem() {
		FoodItemEntity entity = foodItem(5L, "Reject Target", 1);
		when(foodItemRepository.findById(5L)).thenReturn(java.util.Optional.of(entity));
		when(foodItemRepository.save(any(FoodItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		FoodModerationService.FoodModerationItem item = foodModerationService.rejectFoodItem(5L);

		assertThat(item.auditStatus()).isEqualTo("rejected");
		assertThat(entity.getAuditStatus()).isEqualTo("rejected");
	}

	@Test
	void shouldClearReportCount() {
		FoodItemEntity entity = foodItem(6L, "Clear Target", 5);
		entity.setAuditStatus("approved");
		when(foodItemRepository.findById(6L)).thenReturn(java.util.Optional.of(entity));
		when(foodItemRepository.save(any(FoodItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		FoodModerationService.FoodModerationItem item = foodModerationService.clearReportCount(6L);

		assertThat(item.reportCount()).isZero();
		assertThat(entity.getReportCount()).isZero();
	}

	@Test
	void shouldMergeFoodItemRecordsAndRejectDuplicateItem() {
		FoodItemEntity duplicate = foodItem(13L, "Duplicate Noodles", 4);
		FoodItemEntity target = foodItem(14L, "Canonical Noodles", 0);
		target.setAuditStatus("approved");
		when(foodItemRepository.findById(13L)).thenReturn(java.util.Optional.of(duplicate));
		when(foodItemRepository.findById(14L)).thenReturn(java.util.Optional.of(target));
		when(foodRecordRepository.reassignFoodItem(duplicate, target)).thenReturn(3);
		when(foodItemRepository.save(any(FoodItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		FoodModerationService.FoodItemMergeResult result = foodModerationService.mergeFoodItem(13L, 14L);

		assertThat(result.migratedRecordCount()).isEqualTo(3);
		assertThat(result.duplicateItem().auditStatus()).isEqualTo("rejected");
		assertThat(result.duplicateItem().reportCount()).isZero();
		assertThat(result.targetItem().id()).isEqualTo(14L);
		verify(foodRecordRepository).reassignFoodItem(duplicate, target);
	}

	@Test
	void shouldListFoodItemsWithFiltersAndLimit() {
		FoodItemEntity first = foodItem(10L, "Alpha Crackers", 0);
		first.setAuditStatus("approved");
		first.setBrand("Alpha");
		first.setSearchKeywords("alpha crackers snack");
		FoodItemEntity second = foodItem(11L, "Beta Chips", 0);
		second.setAuditStatus("pending");
		second.setBrand("Beta");
		second.setSearchKeywords("beta chips snack");
		when(foodItemRepository.findAll(any(Sort.class))).thenReturn(List.of(second, first));

		List<FoodModerationService.FoodModerationItem> items = foodModerationService.listFoodItems("approved", "alpha", 10);

		assertThat(items).hasSize(1);
		assertThat(items.getFirst().name()).isEqualTo("Alpha Crackers");
		assertThat(items.getFirst().auditStatus()).isEqualTo("approved");
	}

	@Test
	void shouldGetFoodItemById() {
		FoodItemEntity entity = foodItem(12L, "Detail Target", 3);
		entity.setAuditStatus("pending");
		when(foodItemRepository.findById(12L)).thenReturn(java.util.Optional.of(entity));

		FoodModerationService.FoodModerationItem item = foodModerationService.getFoodItem(12L);

		assertThat(item.id()).isEqualTo(12L);
		assertThat(item.name()).isEqualTo("Detail Target");
		assertThat(item.reportCount()).isEqualTo(3);
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
