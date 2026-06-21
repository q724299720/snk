package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.food.FoodFeedbackService;
import com.snk.server.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(FoodFeedbackControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(FoodFeedbackController.class)
class FoodFeedbackControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FoodFeedbackService foodFeedbackService;

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void shouldReportFoodItemAndReturnUpdatedCount() throws Exception {
		when(foodFeedbackService.reportFoodItem(eq(2L), eq(18L), eq("识别错误")))
			.thenReturn(new FoodFeedbackService.FoodReportResult(18L, 4, "approved", 2L, "识别错误"));

		mockMvc.perform(
			post("/api/foods/18/report")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "userId": 2,
					  "reason": "识别错误"
					}
					"""
				)
		)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.foodItemId").value(18))
			.andExpect(jsonPath("$.reportCount").value(4))
			.andExpect(jsonPath("$.auditStatus").value("approved"));
	}

	@Test
	void shouldRejectReportWhenFoodItemIdIsNotPositive() throws Exception {
		mockMvc.perform(
			post("/api/foods/0/report")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "userId": 2,
					  "reason": "识别错误"
					}
					"""
				)
		)
			.andExpect(status().isBadRequest());

		verify(foodFeedbackService, never()).reportFoodItem(anyLong(), anyLong(), anyString());
	}

	@Test
	void shouldRejectReportWhenUserIdIsNotPositive() throws Exception {
		mockMvc.perform(
			post("/api/foods/18/report")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "userId": 0,
					  "reason": null
					}
					"""
				)
		)
			.andExpect(status().isBadRequest());

		verify(foodFeedbackService, never()).reportFoodItem(anyLong(), anyLong(), isNull());
	}
}
