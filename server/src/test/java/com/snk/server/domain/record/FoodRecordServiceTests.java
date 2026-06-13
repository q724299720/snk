package com.snk.server.domain.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordEntity;
import com.snk.server.infrastructure.persistence.record.FoodRecordRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FoodRecordServiceTests {

	@Mock
	private FoodRecordRepository foodRecordRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private FoodItemRepository foodItemRepository;

	@InjectMocks
	private FoodRecordService foodRecordService;

	@Test
	void shouldCreateFoodRecord() throws Exception {
		UserEntity user = new UserEntity();
		setUserId(user, 100L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 200L);

		when(userRepository.findById(100L)).thenReturn(Optional.of(user));
		when(foodItemRepository.findById(200L)).thenReturn(Optional.of(foodItem));
		when(foodRecordRepository.save(any(FoodRecordEntity.class))).thenAnswer(invocation -> {
			FoodRecordEntity entity = invocation.getArgument(0);
			setRecordId(entity, 1L);
			setCreatedAt(entity, OffsetDateTime.parse("2026-06-13T23:30:00Z"));
			if (entity.getRecordTime() == null) {
				entity.setRecordTime(OffsetDateTime.parse("2026-06-13T23:30:00Z"));
			}
			return entity;
		});

		FoodRecordResult result = foodRecordService.createRecord(
			new FoodRecordCreateCommand(
				100L,
				200L,
				"text_search",
				false,
				(short) 4,
				"口味不错",
				OffsetDateTime.parse("2026-06-13T23:29:00Z")
			)
		);

		ArgumentCaptor<FoodRecordEntity> captor = ArgumentCaptor.forClass(FoodRecordEntity.class);
		verify(foodRecordRepository).save(captor.capture());
		FoodRecordEntity saved = captor.getValue();
		assertThat(saved.getUser().getId()).isEqualTo(100L);
		assertThat(saved.getFoodItem().getId()).isEqualTo(200L);
		assertThat(saved.getSourceType()).isEqualTo("text_search");
		assertThat(saved.getRating()).isEqualTo((short) 4);
		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.foodItemId()).isEqualTo(200L);
	}

	private void setUserId(UserEntity entity, Long id) throws Exception {
		Field field = UserEntity.class.getDeclaredField("id");
		field.setAccessible(true);
		field.set(entity, id);
	}

	private void setFoodItemId(FoodItemEntity entity, Long id) throws Exception {
		Field field = FoodItemEntity.class.getDeclaredField("id");
		field.setAccessible(true);
		field.set(entity, id);
	}

	private void setRecordId(FoodRecordEntity entity, Long id) throws Exception {
		Field field = FoodRecordEntity.class.getDeclaredField("id");
		field.setAccessible(true);
		field.set(entity, id);
	}

	private void setCreatedAt(FoodRecordEntity entity, OffsetDateTime value) throws Exception {
		Field field = FoodRecordEntity.class.getDeclaredField("createdAt");
		field.setAccessible(true);
		field.set(entity, value);
	}
}
