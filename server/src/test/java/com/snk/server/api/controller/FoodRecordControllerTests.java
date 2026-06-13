package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.record.FoodRecordResult;
import com.snk.server.domain.record.FoodRecordService;
import com.snk.server.infrastructure.storage.StorageProperties;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(FoodRecordControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(FoodRecordController.class)
class FoodRecordControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FoodRecordService foodRecordService;

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void shouldCreateRecord() throws Exception {
		when(foodRecordService.createRecord(any())).thenReturn(
			new FoodRecordResult(
				1L,
				100L,
				200L,
				"text_search",
				false,
				(short) 5,
				"很好吃",
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				OffsetDateTime.parse("2026-06-13T23:30:00Z")
			)
		);

		mockMvc.perform(
			post("/api/records")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": 100,
					  "foodItemId": 200,
					  "sourceType": "text_search",
					  "isPublic": false,
					  "rating": 5,
					  "comment": "很好吃"
					}
					""")
		)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.userId").value(100))
			.andExpect(jsonPath("$.foodItemId").value(200))
			.andExpect(jsonPath("$.rating").value(5));
	}

	@Test
	void shouldRejectInvalidRating() throws Exception {
		mockMvc.perform(
			post("/api/records")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": 100,
					  "foodItemId": 200,
					  "sourceType": "text_search",
					  "isPublic": false,
					  "rating": 0
					}
					""")
		)
			.andExpect(status().isBadRequest());
	}
}
