package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.record.FoodRecordResult;
import com.snk.server.domain.record.FoodRecordService;
import com.snk.server.domain.record.FoodRecordHistoryItem;
import com.snk.server.infrastructure.storage.StorageProperties;
import java.time.OffsetDateTime;
import java.util.List;
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
	void shouldListRecentRecords() throws Exception {
		when(foodRecordService.listRecentRecords(100L, 10)).thenReturn(
			List.of(
				new FoodRecordHistoryItem(
					1L,
					100L,
					200L,
					"Lays Cucumber Chips",
					"packaged_product",
					"snack",
					"chips",
					"Lays",
					"https://snk.qiuxinmin.cn/images/1.png",
					"text_search",
					false,
					(short) 5,
					"tasty",
					2,
					OffsetDateTime.parse("2026-06-13T23:30:00Z"),
					OffsetDateTime.parse("2026-06-13T23:30:00Z")
				)
			)
		);

		mockMvc.perform(get("/api/records").param("userId", "100"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(1))
			.andExpect(jsonPath("$[0].foodName").value("Lays Cucumber Chips"))
			.andExpect(jsonPath("$[0].foodCoverImageUrl").value("https://snk.qiuxinmin.cn/images/1.png"))
			.andExpect(jsonPath("$[0].rating").value(5));
	}

	@Test
	void shouldRejectRecentRecordsWhenUserIdIsNotPositive() throws Exception {
		mockMvc.perform(get("/api/records").param("userId", "0"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectRecentRecordsWhenLimitIsNotPositive() throws Exception {
		mockMvc.perform(get("/api/records").param("userId", "100").param("limit", "0"))
			.andExpect(status().isBadRequest());
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
				"tasty",
				0,
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
					  "comment": "tasty"
					}
					""")
		)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.userId").value(100))
			.andExpect(jsonPath("$.foodItemId").value(200))
			.andExpect(jsonPath("$.rating").value(5))
			.andExpect(jsonPath("$.likeCount").value(0));
	}

	@Test
	void shouldLikeRecord() throws Exception {
		when(foodRecordService.likeRecord(eq(1L))).thenReturn(
			new FoodRecordResult(
				1L,
				100L,
				200L,
				"text_search",
				false,
				(short) 5,
				"tasty",
				3,
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				OffsetDateTime.parse("2026-06-13T23:30:00Z")
			)
		);

		mockMvc.perform(
			post("/api/records/1/like")
				.contentType(MediaType.APPLICATION_JSON)
		)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.likeCount").value(3));
	}

	@Test
	void shouldRejectLikeWhenRecordIdIsNotPositive() throws Exception {
		when(foodRecordService.likeRecord(anyLong())).thenReturn(
			new FoodRecordResult(
				0L,
				100L,
				200L,
				"text_search",
				false,
				(short) 5,
				"tasty",
				1,
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				OffsetDateTime.parse("2026-06-13T23:30:00Z")
			)
		);

		mockMvc.perform(
			post("/api/records/0/like")
				.contentType(MediaType.APPLICATION_JSON)
		)
			.andExpect(status().isBadRequest());
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

	@Test
	void shouldRejectUnsupportedSourceType() throws Exception {
		when(foodRecordService.createRecord(any())).thenReturn(
			new FoodRecordResult(
				1L,
				100L,
				200L,
				"barcode",
				false,
				(short) 5,
				null,
				0,
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
					  "sourceType": "barcode",
					  "isPublic": false,
					  "rating": 5
					}
					""")
		)
			.andExpect(status().isBadRequest());
	}
}
