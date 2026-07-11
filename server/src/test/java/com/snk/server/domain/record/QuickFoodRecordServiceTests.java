package com.snk.server.domain.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordCommentRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordEntity;
import com.snk.server.infrastructure.persistence.record.FoodRecordImageRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuickFoodRecordServiceTests {

	@Mock FoodRecordRepository foodRecordRepository;
	@Mock FoodRecordImageRepository foodRecordImageRepository;
	@Mock FoodRecordCommentRepository foodRecordCommentRepository;
	@Mock UserRepository userRepository;
	@Mock FoodItemRepository foodItemRepository;

	private FoodRecordService service;

	@BeforeEach
	void setUp() {
		service = new FoodRecordService(
			foodRecordRepository,
			foodRecordImageRepository,
			foodRecordCommentRepository,
			userRepository,
			foodItemRepository
		);
	}

	@Test
	void shouldReturnExistingRecordForRepeatedClientRequestId() throws Exception {
		UUID requestId = UUID.fromString("c6411e4e-b5f5-43d0-9a34-38d54d071b3f");
		UserEntity user = user(100L);
		FoodItemEntity foodItem = foodItem(200L, "麦当劳薯条", "approved", "dish", "meal");
		FoodRecordEntity existing = record(300L, user, foodItem, requestId);
		when(foodRecordRepository.findByUser_IdAndClientRequestId(100L, requestId))
			.thenReturn(Optional.of(existing));

		FoodRecordResult result = service.createQuickRecord(command(requestId));

		assertThat(result.id()).isEqualTo(300L);
		verify(foodRecordRepository, never()).save(any(FoodRecordEntity.class));
		verify(foodItemRepository, never()).save(any(FoodItemEntity.class));
	}

	@Test
	void shouldMakeStandardRecordCreationIdempotent() throws Exception {
		UUID requestId = UUID.fromString("b462a65b-b346-4a6d-bd87-c2022897544a");
		UserEntity user = user(100L);
		FoodItemEntity foodItem = foodItem(200L, "标准条目", "approved", "dish", "meal");
		FoodRecordEntity existing = record(300L, user, foodItem, requestId);
		when(foodRecordRepository.findByUser_IdAndClientRequestId(100L, requestId))
			.thenReturn(Optional.of(existing));

		FoodRecordResult result = service.createRecord(
			new FoodRecordCreateCommand(
				100L,
				200L,
				"text_search",
				false,
				(short) 5,
				null,
				null,
				List.of(),
				requestId
			)
		);

		assertThat(result.id()).isEqualTo(300L);
		verify(foodRecordRepository, never()).save(any(FoodRecordEntity.class));
	}

	@Test
	void shouldCreatePendingUncategorizedItemAndPrivateRecord() throws Exception {
		UUID requestId = UUID.fromString("c6411e4e-b5f5-43d0-9a34-38d54d071b3f");
		UserEntity user = user(100L);
		when(foodRecordRepository.findByUser_IdAndClientRequestId(100L, requestId))
			.thenReturn(Optional.empty());
		when(userRepository.findById(100L)).thenReturn(Optional.of(user));
		when(foodItemRepository.findFirstApprovedByNormalizedName("麦当劳 薯条"))
			.thenReturn(Optional.empty());
		when(foodItemRepository.findFirstCreatorPendingByNormalizedName(100L, "麦当劳 薯条"))
			.thenReturn(Optional.empty());
		when(foodItemRepository.save(any(FoodItemEntity.class))).thenAnswer(invocation -> {
			FoodItemEntity entity = invocation.getArgument(0);
			setId(entity, 200L);
			return entity;
		});
		when(foodRecordRepository.save(any(FoodRecordEntity.class))).thenAnswer(invocation -> {
			FoodRecordEntity entity = invocation.getArgument(0);
			setId(entity, 300L);
			setCreatedAt(entity, OffsetDateTime.parse("2026-07-11T12:00:00Z"));
			return entity;
		});

		FoodRecordResult result = service.createQuickRecord(command(requestId));

		assertThat(result.foodItemId()).isEqualTo(200L);
		assertThat(result.isPublic()).isFalse();
		org.mockito.ArgumentCaptor<FoodItemEntity> foodCaptor = org.mockito.ArgumentCaptor.forClass(FoodItemEntity.class);
		verify(foodItemRepository).save(foodCaptor.capture());
		assertThat(foodCaptor.getValue().getName()).isEqualTo("麦当劳 薯条");
		assertThat(foodCaptor.getValue().getItemType()).isEqualTo("unknown");
		assertThat(foodCaptor.getValue().getCategory()).isEqualTo("uncategorized");
		assertThat(foodCaptor.getValue().getAuditStatus()).isEqualTo("pending");
		org.mockito.ArgumentCaptor<FoodRecordEntity> recordCaptor = org.mockito.ArgumentCaptor.forClass(FoodRecordEntity.class);
		verify(foodRecordRepository).save(recordCaptor.capture());
		assertThat(recordCaptor.getValue().getClientRequestId()).isEqualTo(requestId);
	}

	private QuickFoodRecordCreateCommand command(UUID requestId) {
		return new QuickFoodRecordCreateCommand(
			requestId,
			100L,
			"  麦当劳  薯条  ",
			false,
			(short) 5,
			"热且脆",
			null,
			List.of()
		);
	}

	private UserEntity user(Long id) throws Exception {
		UserEntity entity = new UserEntity();
		setId(entity, id);
		return entity;
	}

	private FoodItemEntity foodItem(Long id, String name, String auditStatus, String itemType, String category) throws Exception {
		FoodItemEntity entity = new FoodItemEntity();
		setId(entity, id);
		entity.setName(name);
		entity.setAuditStatus(auditStatus);
		entity.setItemType(itemType);
		entity.setCategory(category);
		return entity;
	}

	private FoodRecordEntity record(Long id, UserEntity user, FoodItemEntity foodItem, UUID requestId) throws Exception {
		FoodRecordEntity entity = new FoodRecordEntity();
		setId(entity, id);
		setCreatedAt(entity, OffsetDateTime.parse("2026-07-11T12:00:00Z"));
		entity.setUser(user);
		entity.setFoodItem(foodItem);
		entity.setClientRequestId(requestId);
		entity.setSourceType("manual");
		entity.setPublic(false);
		entity.setRating((short) 5);
		entity.setRecordTime(OffsetDateTime.parse("2026-07-11T12:00:00Z"));
		return entity;
	}

	private void setId(Object entity, Long id) throws Exception {
		Field field = entity.getClass().getDeclaredField("id");
		field.setAccessible(true);
		field.set(entity, id);
	}

	private void setCreatedAt(FoodRecordEntity entity, OffsetDateTime createdAt) throws Exception {
		Field field = FoodRecordEntity.class.getDeclaredField("createdAt");
		field.setAccessible(true);
		field.set(entity, createdAt);
	}
}
