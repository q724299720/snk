package com.snk.server.domain.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.food.FoodItemReportEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemReportRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FoodFeedbackServiceTests {

	@Mock
	private FoodItemRepository foodItemRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private FoodItemReportRepository foodItemReportRepository;

	@InjectMocks
	private FoodFeedbackService foodFeedbackService;

	@Test
	void shouldIncrementReportCountForExistingFoodItem() {
		UserEntity user = new UserEntity();
		setField(user, "id", 2L);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		FoodItemEntity foodItem = new FoodItemEntity();
		setField(foodItem, "id", 18L);
		foodItem.setReportCount(3);
		foodItem.setAuditStatus("pending");
		when(foodItemRepository.findById(18L)).thenReturn(Optional.of(foodItem));
		when(foodItemRepository.save(any(FoodItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		FoodFeedbackService.FoodReportResult result = foodFeedbackService.reportFoodItem(2L, 18L, "识别错误");

		assertThat(result.foodItemId()).isEqualTo(18L);
		assertThat(result.reportCount()).isEqualTo(4);
		assertThat(result.auditStatus()).isEqualTo("pending");
		assertThat(result.reporterUserId()).isEqualTo(2L);
		assertThat(result.reason()).isEqualTo("识别错误");

		ArgumentCaptor<FoodItemReportEntity> reportCaptor = ArgumentCaptor.forClass(FoodItemReportEntity.class);
		verify(foodItemReportRepository).save(reportCaptor.capture());
		FoodItemReportEntity report = reportCaptor.getValue();
		assertThat(report.getFoodItem()).isSameAs(foodItem);
		assertThat(report.getReporterUser()).isSameAs(user);
		assertThat(report.getReason()).isEqualTo("识别错误");
	}

	@Test
	void shouldRejectUnknownUser() {
		when(userRepository.findById(2L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> foodFeedbackService.reportFoodItem(2L, 18L, null))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("404 NOT_FOUND");
	}

	private void setField(Object target, String fieldName, Object value) {
		try {
			var field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
