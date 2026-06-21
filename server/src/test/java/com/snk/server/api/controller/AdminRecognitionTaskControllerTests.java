package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.recognition.ImageRecognitionCandidateSnapshot;
import com.snk.server.domain.recognition.RecognitionTaskResult;
import com.snk.server.domain.recognition.RecognitionTaskService;
import com.snk.server.infrastructure.storage.StorageProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(AdminRecognitionTaskControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(AdminRecognitionTaskController.class)
class AdminRecognitionTaskControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RecognitionTaskService recognitionTaskService;

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void shouldListRecognitionTasks() throws Exception {
		when(recognitionTaskService.listTasks(eq("completed"), eq(9L), eq(10)))
			.thenReturn(List.of(recognitionTask(18L, 9L, "completed")));

		mockMvc.perform(get("/api/admin/recognition-tasks")
			.param("status", "completed")
			.param("userId", "9")
			.param("limit", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(18))
			.andExpect(jsonPath("$[0].status").value("completed"))
			.andExpect(jsonPath("$[0].selectedFoodItemId").value(11));
	}

	@Test
	void shouldRejectRecognitionTaskListWhenLimitIsNotPositive() throws Exception {
		mockMvc.perform(get("/api/admin/recognition-tasks").param("limit", "0"))
			.andExpect(status().isBadRequest());

		verify(recognitionTaskService, never()).listTasks(any(), any(), anyInt());
	}

	@Test
	void shouldReturnRecognitionTaskDetail() throws Exception {
		when(recognitionTaskService.getTask(20L))
			.thenReturn(recognitionTask(20L, 5L, "failed"));

		mockMvc.perform(get("/api/admin/recognition-tasks/20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("failed"))
			.andExpect(jsonPath("$.statusReason").value("image recognition provider is not configured"));
	}

	@Test
	void shouldRejectDetailWhenTaskIdIsNotPositive() throws Exception {
		mockMvc.perform(get("/api/admin/recognition-tasks/0"))
			.andExpect(status().isBadRequest());

		verify(recognitionTaskService, never()).getTask(any());
	}

	private RecognitionTaskResult recognitionTask(Long id, Long userId, String status) {
		return new RecognitionTaskResult(
			id,
			userId,
			"/uploads/images/demo.png",
			status,
			List.of(
				new ImageRecognitionCandidateSnapshot(
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
			11L,
			new BigDecimal("0.8500"),
			OffsetDateTime.parse("2026-06-14T12:00:00Z"),
			OffsetDateTime.parse("2026-06-14T12:00:03Z"),
			"image recognition provider is not configured"
		);
	}
}
