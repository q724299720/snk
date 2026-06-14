package com.snk.server.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.recognition.RecognitionTaskRepository;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTests {

	@Mock
	private FoodItemRepository foodItemRepository;

	@Mock
	private RecognitionTaskRepository recognitionTaskRepository;

	@Mock
	private ReviewConfigWordRepository reviewConfigWordRepository;

	@InjectMocks
	private AdminStatsService adminStatsService;

	@Test
	void shouldSummarizeAdminStats() {
		when(foodItemRepository.countByAuditStatus("pending")).thenReturn(3L);
		when(foodItemRepository.countByAuditStatus("approved")).thenReturn(12L);
		when(foodItemRepository.countByAuditStatus("rejected")).thenReturn(2L);
		when(foodItemRepository.countByReportCountGreaterThanEqual(1)).thenReturn(4L);
		when(recognitionTaskRepository.countByStatus("processing")).thenReturn(1L);
		when(recognitionTaskRepository.countByStatus("completed")).thenReturn(5L);
		when(recognitionTaskRepository.countByStatus("failed")).thenReturn(2L);
		when(reviewConfigWordRepository.countByEnabled(true)).thenReturn(8L);
		when(reviewConfigWordRepository.countByEnabled(false)).thenReturn(1L);

		AdminStatsService.AdminStatsResult result = adminStatsService.getStats();

		assertThat(result.totalFoodItems()).isEqualTo(17L);
		assertThat(result.pendingFoodItems()).isEqualTo(3L);
		assertThat(result.reportedFoodItems()).isEqualTo(4L);
		assertThat(result.totalRecognitionTasks()).isEqualTo(8L);
		assertThat(result.enabledReviewWords()).isEqualTo(8L);
	}
}
