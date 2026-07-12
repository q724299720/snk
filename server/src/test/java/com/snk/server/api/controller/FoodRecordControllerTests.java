package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.record.FoodRecordUpdateCommand;
import com.snk.server.domain.record.FoodRecordResult;
import com.snk.server.domain.record.FoodRecordService;
import com.snk.server.domain.record.FoodRecordCommentResult;
import com.snk.server.domain.record.FoodRecordHistoryItem;
import com.snk.server.domain.record.FoodRecordImageValue;
import com.snk.server.domain.record.FoodRecordCreateCommand;
import com.snk.server.domain.record.QuickFoodRecordCreateCommand;
import com.snk.server.infrastructure.storage.StorageProperties;
import com.snk.server.infrastructure.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.List;
import org.mockito.ArgumentCaptor;
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

	@MockBean
	private CurrentUser currentUser;

	@org.junit.jupiter.api.BeforeEach
	void useTrustedAccountIdentity() {
		when(currentUser.requiredUserId()).thenReturn(100L);
	}

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void shouldGetOwnRecordDetail() throws Exception {
		when(foodRecordService.getRecordForUser(1L, 100L)).thenReturn(
			new FoodRecordResult(
				1L,
				100L,
				200L,
				"text_search",
				false,
				(short) 4,
				"old comment",
				0,
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				List.of(
					new FoodRecordImageValue(
						"https://snk.qiuxinmin.cn/uploads/records/noodle.jpg",
						"https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg"
					)
				)
			)
		);

		mockMvc.perform(get("/api/records/1").param("userId", "100"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.userId").value(100))
			.andExpect(jsonPath("$.rating").value(4))
			.andExpect(jsonPath("$.comment").value("old comment"))
			.andExpect(jsonPath("$.images[0].thumbnailUrl")
				.value("https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg"));
	}

	@Test
	void shouldUpdateOwnRecord() throws Exception {
		when(foodRecordService.updateRecord(any())).thenReturn(
			new FoodRecordResult(
				1L,
				100L,
				200L,
				"text_search",
				true,
				(short) 5,
				"better after edit",
				0,
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				List.of()
			)
		);

		mockMvc.perform(
			put("/api/records/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": 100,
					  "rating": 5,
					  "comment": "better after edit",
					  "isPublic": true,
					  "images": [
					    {
					      "imageUrl": "https://snk.qiuxinmin.cn/uploads/records/new.jpg",
					      "thumbnailUrl": "https://snk.qiuxinmin.cn/uploads/records/new-thumb.jpg"
					    }
					  ]
					}
					""")
		)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.isPublic").value(true))
			.andExpect(jsonPath("$.rating").value(5))
			.andExpect(jsonPath("$.comment").value("better after edit"));

		ArgumentCaptor<FoodRecordUpdateCommand> captor = ArgumentCaptor.forClass(FoodRecordUpdateCommand.class);
		verify(foodRecordService).updateRecord(captor.capture());
		FoodRecordUpdateCommand command = captor.getValue();
		org.assertj.core.api.Assertions.assertThat(command.recordId()).isEqualTo(1L);
		org.assertj.core.api.Assertions.assertThat(command.userId()).isEqualTo(100L);
		org.assertj.core.api.Assertions.assertThat(command.rating()).isEqualTo((short) 5);
		org.assertj.core.api.Assertions.assertThat(command.comment()).isEqualTo("better after edit");
		org.assertj.core.api.Assertions.assertThat(command.isPublic()).isTrue();
		org.assertj.core.api.Assertions.assertThat(command.images()).hasSize(1);
		org.assertj.core.api.Assertions.assertThat(command.images().getFirst().thumbnailUrl())
			.isEqualTo("https://snk.qiuxinmin.cn/uploads/records/new-thumb.jpg");
	}

	@Test
	void shouldDeleteOwnRecord() throws Exception {
		mockMvc.perform(
			delete("/api/records/1").param("userId", "100")
		)
			.andExpect(status().isNoContent());

		verify(foodRecordService).deleteRecord(1L, 100L);
	}

	@Test
	void shouldRejectDeleteWhenRecordIdIsNotPositive() throws Exception {
		mockMvc.perform(
			delete("/api/records/0").param("userId", "100")
		)
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldIgnoreDeleteUserIdQueryParameter() throws Exception {
		mockMvc.perform(
			delete("/api/records/1").param("userId", "0")
		)
			.andExpect(status().isNoContent());
	}

	@Test
	void shouldRejectUpdateWithOverlongRecordComment() throws Exception {
		mockMvc.perform(
			put("/api/records/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": 100,
					  "rating": 5,
					  "comment": "%s",
					  "isPublic": false
					}
					""".formatted("a".repeat(501)))
		)
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldListRecentRecords() throws Exception {
		when(foodRecordService.listRecentRecords(100L, 0, 20)).thenReturn(
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
					OffsetDateTime.parse("2026-06-13T23:30:00Z"),
					List.of(
						new FoodRecordImageValue(
							"https://snk.qiuxinmin.cn/uploads/records/chips.jpg",
							"https://snk.qiuxinmin.cn/uploads/records/chips-thumb.jpg"
						)
					)
				)
			)
		);

		mockMvc.perform(get("/api/records").param("userId", "100"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(1))
			.andExpect(jsonPath("$[0].foodName").value("Lays Cucumber Chips"))
			.andExpect(jsonPath("$[0].foodCoverImageUrl").value("https://snk.qiuxinmin.cn/images/1.png"))
			.andExpect(jsonPath("$[0].rating").value(5))
			.andExpect(jsonPath("$[0].images[0].thumbnailUrl")
				.value("https://snk.qiuxinmin.cn/uploads/records/chips-thumb.jpg"));
	}

	@Test
	void shouldListPublicRecords() throws Exception {
		when(foodRecordService.listPublicRecords(10)).thenReturn(
			List.of(
				new FoodRecordHistoryItem(
					2L,
					101L,
					201L,
					"Kangshifu Beef Noodles",
					"packaged_product",
					"instant_food",
					"noodles",
					"Kangshifu",
					"https://snk.qiuxinmin.cn/images/noodle.png",
					"text_search",
					true,
					(short) 4,
					"share this",
					7,
					OffsetDateTime.parse("2026-06-13T23:40:00Z"),
					OffsetDateTime.parse("2026-06-13T23:40:00Z"),
					List.of(
						new FoodRecordImageValue(
							"https://snk.qiuxinmin.cn/uploads/records/noodle.jpg",
							"https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg"
						)
					)
				)
			)
		);

		mockMvc.perform(get("/api/records/public"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(2))
			.andExpect(jsonPath("$[0].isPublic").value(true))
			.andExpect(jsonPath("$[0].foodName").value("Kangshifu Beef Noodles"))
			.andExpect(jsonPath("$[0].images[0].thumbnailUrl")
				.value("https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg"));
	}

	@Test
	void shouldIgnoreRecentRecordsUserIdQueryParameter() throws Exception {
		mockMvc.perform(get("/api/records").param("userId", "0"))
			.andExpect(status().isOk());
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
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				List.of(
					new FoodRecordImageValue(
						"https://snk.qiuxinmin.cn/uploads/records/noodle.jpg",
						"https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg"
					)
				)
			)
		);

		mockMvc.perform(
			post("/api/records")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "clientRequestId": "b462a65b-b346-4a6d-bd87-c2022897544a",
					  "userId": 999,
					  "foodItemId": 200,
					  "sourceType": "text_search",
					  "isPublic": false,
					  "rating": 5,
					  "comment": "tasty",
					  "images": [
					    {
					      "imageUrl": "https://snk.qiuxinmin.cn/uploads/records/noodle.jpg",
					      "thumbnailUrl": "https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg"
					    }
					  ]
					}
					""")
		)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.userId").value(100))
			.andExpect(jsonPath("$.foodItemId").value(200))
			.andExpect(jsonPath("$.rating").value(5))
			.andExpect(jsonPath("$.likeCount").value(0))
			.andExpect(jsonPath("$.images[0].imageUrl")
				.value("https://snk.qiuxinmin.cn/uploads/records/noodle.jpg"));

		ArgumentCaptor<FoodRecordCreateCommand> commandCaptor = ArgumentCaptor.forClass(FoodRecordCreateCommand.class);
		verify(foodRecordService).createRecord(commandCaptor.capture());
		org.assertj.core.api.Assertions.assertThat(commandCaptor.getValue().clientRequestId())
			.isEqualTo(java.util.UUID.fromString("b462a65b-b346-4a6d-bd87-c2022897544a"));
		org.assertj.core.api.Assertions.assertThat(commandCaptor.getValue().userId()).isEqualTo(100L);
	}

	@Test
	void shouldCreateQuickRecord() throws Exception {
		when(foodRecordService.createQuickRecord(any())).thenReturn(
			new FoodRecordResult(
				1L, 100L, 200L, "manual", false, (short) 5, "热且脆", 0,
				OffsetDateTime.parse("2026-07-11T12:00:00Z"),
				OffsetDateTime.parse("2026-07-11T12:00:00Z"),
				List.of()
			)
		);

		mockMvc.perform(
			post("/api/records/quick")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "clientRequestId": "c6411e4e-b5f5-43d0-9a34-38d54d071b3f",
					  "userId": 100,
					  "name": "麦当劳薯条",
					  "rating": 5,
					  "comment": "热且脆"
					}
					""")
		)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.foodItemId").value(200))
			.andExpect(jsonPath("$.isPublic").value(false));

		ArgumentCaptor<QuickFoodRecordCreateCommand> captor = ArgumentCaptor.forClass(QuickFoodRecordCreateCommand.class);
		verify(foodRecordService).createQuickRecord(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().name()).isEqualTo("麦当劳薯条");
		org.assertj.core.api.Assertions.assertThat(captor.getValue().clientRequestId())
			.isEqualTo(java.util.UUID.fromString("c6411e4e-b5f5-43d0-9a34-38d54d071b3f"));
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
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				List.of()
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
	void shouldListRecordComments() throws Exception {
		when(foodRecordService.listComments(1L, 10)).thenReturn(
			List.of(
				new FoodRecordCommentResult(
					9L,
					1L,
					100L,
					"看起来不错",
					OffsetDateTime.parse("2026-06-21T12:00:00Z")
				)
			)
		);

		mockMvc.perform(get("/api/records/1/comments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(9))
			.andExpect(jsonPath("$[0].recordId").value(1))
			.andExpect(jsonPath("$[0].userId").value(100))
			.andExpect(jsonPath("$[0].content").value("看起来不错"));
	}

	@Test
	void shouldCreateRecordComment() throws Exception {
		when(foodRecordService.createComment(eq(1L), eq(100L), eq("看起来不错"))).thenReturn(
			new FoodRecordCommentResult(
				9L,
				1L,
				100L,
				"看起来不错",
				OffsetDateTime.parse("2026-06-21T12:00:00Z")
			)
		);

		mockMvc.perform(
			post("/api/records/1/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": 100,
					  "content": "看起来不错"
					}
					""")
		)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(9))
			.andExpect(jsonPath("$.recordId").value(1))
			.andExpect(jsonPath("$.userId").value(100))
			.andExpect(jsonPath("$.content").value("看起来不错"));
	}

	@Test
	void shouldRejectBlankRecordComment() throws Exception {
		mockMvc.perform(
			post("/api/records/1/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "userId": 100,
					  "content": "   "
					}
					""")
		)
			.andExpect(status().isBadRequest());
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
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				List.of()
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
	void shouldRejectOverlongRecordComment() throws Exception {
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
					  "comment": "%s"
					}
					""".formatted("a".repeat(501)))
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
				OffsetDateTime.parse("2026-06-13T23:30:00Z"),
				List.of()
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
