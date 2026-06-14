package com.snk.server.domain.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ManualFoodItemServiceTests {

	@Mock
	private FoodItemRepository foodItemRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private ManualFoodItemService manualFoodItemService;

	@Test
	void shouldCreatePendingFoodItemForUserGeneratedFallback() {
		UserEntity creator = new UserEntity();
		creator.setNickname("guest");
		when(userRepository.findById(eq(2L))).thenReturn(Optional.of(creator));
		when(foodItemRepository.save(any(FoodItemEntity.class))).thenAnswer(invocation -> {
			FoodItemEntity entity = invocation.getArgument(0);
			setField(entity, "id", 21L);
			return entity;
		});

		FoodSearchItem result = manualFoodItemService.createPendingItem(
			new CreateManualFoodItemCommand(2L, " 杨枝 鲜花饼 ", "dish", "dessert", "cake", "SNK Bakery")
		);

		assertThat(result.id()).isEqualTo(21L);
		assertThat(result.auditStatus()).isEqualTo("pending");
		assertThat(result.brand()).isEqualTo("SNK Bakery");
		assertThat(result.category()).isEqualTo("dessert");
	}

	@Test
	void shouldRejectInvalidItemType() {
		UserEntity creator = new UserEntity();
		when(userRepository.findById(eq(2L))).thenReturn(Optional.of(creator));

		assertThatThrownBy(
			() -> manualFoodItemService.createPendingItem(
				new CreateManualFoodItemCommand(2L, "杨枝鲜花饼", "unknown_type", "dessert", null, null)
			)
		)
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("400 BAD_REQUEST");
	}

	private void setField(FoodItemEntity entity, String fieldName, Object value) {
		try {
			var field = FoodItemEntity.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(entity, value);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
