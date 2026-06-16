package com.snk.server.domain.recognition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snk.server.domain.food.FoodSearchItem;
import com.snk.server.domain.food.FoodSearchResult;
import com.snk.server.domain.food.FoodSearchService;
import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.recognition.RecognitionTaskEntity;
import com.snk.server.infrastructure.persistence.recognition.RecognitionTaskRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RecognitionTaskServiceTests {

	@Mock
	private RecognitionTaskRepository recognitionTaskRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private FoodItemRepository foodItemRepository;

	@Mock
	private ImageRecognitionTaskProvider imageRecognitionTaskProvider;

	@Mock
	private FoodSearchService foodSearchService;

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	private RecognitionTaskService recognitionTaskService;

	@Test
	void shouldCreateCompletedRecognitionTaskWhenProviderFindsCandidates() {
		UserEntity user = new UserEntity();
		setUserId(user, 2L);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(recognitionTaskRepository.save(any(RecognitionTaskEntity.class))).thenAnswer(invocation -> {
			RecognitionTaskEntity entity = invocation.getArgument(0);
			setTaskId(entity, entity.getId() == null ? 8L : entity.getId());
			if (entity.getCreatedAt() == null) {
				setCreatedAt(entity, OffsetDateTime.parse("2026-06-14T12:00:00Z"));
			}
			return entity;
		});
		when(imageRecognitionTaskProvider.recognize(eq("/uploads/images/demo.png")))
			.thenReturn(new ImageRecognitionTaskProviderResult(List.of("LaysCucumberChips"), new BigDecimal("0.8500")));
		when(foodSearchService.search(eq("LaysCucumberChips")))
			.thenReturn(
				new FoodSearchResult(
					List.of(
						new FoodSearchItem(
							11L,
							"Lays Cucumber Chips",
							"packaged_product",
							"snack",
							"chips",
							"Lays",
							"6900000000011",
							null,
							"approved"
						)
					),
					"strong"
				)
			);
		FoodItemEntity selectedFoodItem = new FoodItemEntity();
		setFoodItemId(selectedFoodItem, 11L);
		when(foodItemRepository.findById(11L)).thenReturn(Optional.of(selectedFoodItem));

		RecognitionTaskResult result = recognitionTaskService.createTask(
			new ImageRecognitionTaskCommand(2L, "/uploads/images/demo.png", null)
		);

		assertThat(result.id()).isEqualTo(8L);
		assertThat(result.status()).isEqualTo("completed");
		assertThat(result.topCandidates()).hasSize(1);
		assertThat(result.topCandidates().getFirst().name()).isEqualTo("Lays Cucumber Chips");
		assertThat(result.selectedFoodItemId()).isEqualTo(11L);
		assertThat(result.confidence()).isEqualByComparingTo("0.8500");
	}

	@Test
	void shouldCreateFailedRecognitionTaskWhenProviderIsUnavailable() {
		UserEntity user = new UserEntity();
		setUserId(user, 2L);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(recognitionTaskRepository.save(any(RecognitionTaskEntity.class))).thenAnswer(invocation -> {
			RecognitionTaskEntity entity = invocation.getArgument(0);
			setTaskId(entity, entity.getId() == null ? 9L : entity.getId());
			if (entity.getCreatedAt() == null) {
				setCreatedAt(entity, OffsetDateTime.parse("2026-06-14T12:00:00Z"));
			}
			return entity;
		});
		when(imageRecognitionTaskProvider.recognize(eq("/uploads/images/demo.png")))
			.thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "image recognition provider is not configured"));

		RecognitionTaskResult result = recognitionTaskService.createTask(
			new ImageRecognitionTaskCommand(2L, "/uploads/images/demo.png", null)
		);

		assertThat(result.id()).isEqualTo(9L);
		assertThat(result.status()).isEqualTo("failed");
		assertThat(result.topCandidates()).isEmpty();
		assertThat(result.statusReason()).isEqualTo("image recognition provider is not configured");
	}

	@Test
	void shouldRejectBlankImageUrl() {
		assertThatThrownBy(() -> recognitionTaskService.createTask(new ImageRecognitionTaskCommand(2L, " ", null)))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("400 BAD_REQUEST");
	}

	@Test
	void shouldPreferHintQueryBeforeProviderQuery() {
		UserEntity user = new UserEntity();
		setUserId(user, 2L);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(recognitionTaskRepository.save(any(RecognitionTaskEntity.class))).thenAnswer(invocation -> {
			RecognitionTaskEntity entity = invocation.getArgument(0);
			setTaskId(entity, entity.getId() == null ? 10L : entity.getId());
			if (entity.getCreatedAt() == null) {
				setCreatedAt(entity, OffsetDateTime.parse("2026-06-14T12:00:00Z"));
			}
			return entity;
		});
		when(imageRecognitionTaskProvider.recognize(eq("/uploads/images/demo.png")))
			.thenReturn(new ImageRecognitionTaskProviderResult(List.of("乐事黄瓜味"), new BigDecimal("0.8500")));
		when(foodSearchService.search(eq("乐事 薯片 黄瓜味")))
			.thenReturn(
				new FoodSearchResult(
					List.of(
						new FoodSearchItem(
							12L,
							"Lays Cucumber Chips",
							"packaged_product",
							"snack",
							"chips",
							"Lays",
							"6900000000011",
							null,
							"approved"
						)
					),
					"strong"
				)
			);
		FoodItemEntity selectedFoodItem = new FoodItemEntity();
		setFoodItemId(selectedFoodItem, 12L);
		when(foodItemRepository.findById(12L)).thenReturn(Optional.of(selectedFoodItem));

		RecognitionTaskResult result = recognitionTaskService.createTask(
			new ImageRecognitionTaskCommand(2L, "/uploads/images/demo.png", " 乐事 薯片 黄瓜味 ")
		);

		assertThat(result.id()).isEqualTo(10L);
		assertThat(result.status()).isEqualTo("completed");
		assertThat(result.selectedFoodItemId()).isEqualTo(12L);
		assertThat(result.topCandidates()).hasSize(1);
		assertThat(result.topCandidates().getFirst().name()).isEqualTo("Lays Cucumber Chips");
	}

	@Test
	void shouldListRecognitionTasksWithFilters() {
		UserEntity user = new UserEntity();
		setUserId(user, 7L);
		RecognitionTaskEntity entity = recognitionTask("processing", 7L, 18L, user);
		when(recognitionTaskRepository.findByStatusAndUser_IdOrderByCreatedAtDesc(eq("processing"), eq(7L), any()))
			.thenReturn(new PageImpl<>(List.of(entity)));

		List<RecognitionTaskResult> results = recognitionTaskService.listTasks("processing", 7L, 10);

		assertThat(results).hasSize(1);
		assertThat(results.getFirst().id()).isEqualTo(18L);
		assertThat(results.getFirst().status()).isEqualTo("processing");
	}

	private void setTaskId(RecognitionTaskEntity entity, Long id) {
		try {
			var field = RecognitionTaskEntity.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private void setCreatedAt(RecognitionTaskEntity entity, OffsetDateTime createdAt) {
		try {
			var field = RecognitionTaskEntity.class.getDeclaredField("createdAt");
			field.setAccessible(true);
			field.set(entity, createdAt);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private void setUserId(UserEntity entity, Long id) {
		try {
			var field = UserEntity.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private void setFoodItemId(FoodItemEntity entity, Long id) {
		try {
			var field = FoodItemEntity.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private RecognitionTaskEntity recognitionTask(String status, Long userId, Long taskId, UserEntity user) {
		RecognitionTaskEntity entity = new RecognitionTaskEntity();
		setTaskId(entity, taskId);
		setCreatedAt(entity, OffsetDateTime.parse("2026-06-14T12:00:00Z"));
		entity.setStatus(status);
		entity.setInputImageUrl("/uploads/images/demo.png");
		entity.setTopCandidates("[]");
		entity.setUser(user);
		return entity;
	}
}
