package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import com.snk.server.domain.food.FoodModerationService;
import com.snk.server.domain.food.FoodModerationService.FoodModerationItem;
import com.snk.server.domain.food.FoodFeedbackService;
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
import org.springframework.test.web.servlet.MockMvc;

@Import(AdminFoodItemControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(AdminFoodItemController.class)
class AdminFoodItemControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FoodModerationService foodModerationService;

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
	void shouldListFoodItems() throws Exception {
		when(foodModerationService.listFoodItems(eq("approved"), eq("alpha"), eq(10)))
			.thenReturn(List.of(moderationItem(7L, "Alpha Chips", 2, "approved")));

		mockMvc.perform(get("/api/admin/food-items")
			.param("auditStatus", "approved")
			.param("q", "alpha")
			.param("limit", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Alpha Chips"))
			.andExpect(jsonPath("$[0].auditStatus").value("approved"));
	}

	@Test
	void shouldReturnFoodItemDetail() throws Exception {
		when(foodModerationService.getFoodItem(8L))
			.thenReturn(moderationItem(8L, "Detail Item", 1, "pending"));

		mockMvc.perform(get("/api/admin/food-items/8"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(8))
			.andExpect(jsonPath("$.name").value("Detail Item"))
			.andExpect(jsonPath("$.auditStatus").value("pending"));
	}

	@Test
	void shouldReturnPendingItems() throws Exception {
		when(foodModerationService.listPendingItems())
			.thenReturn(List.of(moderationItem(1L, "Pending Item", 0, "pending")));

		mockMvc.perform(get("/api/admin/food-items/pending"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Pending Item"))
			.andExpect(jsonPath("$[0].auditStatus").value("pending"))
			.andExpect(jsonPath("$[0].reportCount").value(0));
	}

	@Test
	void shouldReturnReportedItemsWithThresholdParameter() throws Exception {
		when(foodModerationService.listReportedItems(eq(2)))
			.thenReturn(List.of(moderationItem(2L, "Reported Item", 3, "approved")));

		mockMvc.perform(get("/api/admin/food-items/reported").param("minReportCount", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Reported Item"))
			.andExpect(jsonPath("$[0].reportCount").value(3));
	}

	@Test
	void shouldReturnFoodItemReports() throws Exception {
		when(foodFeedbackService.listFoodItemReports(18L))
			.thenReturn(List.of(new FoodFeedbackService.FoodItemReportItem(
				9L,
				18L,
				2L,
				"识别错误",
				OffsetDateTime.parse("2026-06-21T12:00:00Z")
			)));

		mockMvc.perform(get("/api/admin/food-items/18/reports"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(9))
			.andExpect(jsonPath("$[0].foodItemId").value(18))
			.andExpect(jsonPath("$[0].reporterUserId").value(2))
			.andExpect(jsonPath("$[0].reason").value("识别错误"));
	}

	@Test
	void shouldApproveFoodItem() throws Exception {
		when(foodModerationService.approveFoodItem(3L))
			.thenReturn(moderationItem(3L, "Approved Item", 1, "approved"));

		mockMvc.perform(post("/api/admin/food-items/3/approve"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Approved Item"))
			.andExpect(jsonPath("$.auditStatus").value("approved"));
	}

	@Test
	void shouldRejectApproveWhenFoodItemIdIsNotPositive() throws Exception {
		mockMvc.perform(post("/api/admin/food-items/0/approve"))
			.andExpect(status().isBadRequest());

		verify(foodModerationService, never()).approveFoodItem(anyLong());
	}

	@Test
	void shouldRejectFoodItem() throws Exception {
		when(foodModerationService.rejectFoodItem(4L))
			.thenReturn(moderationItem(4L, "Rejected Item", 2, "rejected"));

		mockMvc.perform(post("/api/admin/food-items/4/reject"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Rejected Item"))
			.andExpect(jsonPath("$.auditStatus").value("rejected"));
	}

	@Test
	void shouldClearReportCount() throws Exception {
		when(foodModerationService.clearReportCount(5L))
			.thenReturn(moderationItem(5L, "Cleared Item", 0, "approved"));

		mockMvc.perform(post("/api/admin/food-items/5/clear-reports"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Cleared Item"))
			.andExpect(jsonPath("$.reportCount").value(0));
	}

	@Test
	void shouldMergeFoodItem() throws Exception {
		when(foodModerationService.mergeFoodItem(6L, 7L))
			.thenReturn(new FoodModerationService.FoodItemMergeResult(
				moderationItem(6L, "Duplicate Item", 0, "rejected"),
				moderationItem(7L, "Target Item", 0, "approved"),
				3
			));

		mockMvc.perform(post("/api/admin/food-items/6/merge")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"targetFoodItemId\":7}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.duplicateItem.auditStatus").value("rejected"))
			.andExpect(jsonPath("$.targetItem.id").value(7))
			.andExpect(jsonPath("$.migratedRecordCount").value(3));
	}

	@Test
	void shouldRejectMergeWhenTargetFoodItemIdIsNotPositive() throws Exception {
		mockMvc.perform(post("/api/admin/food-items/6/merge")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"targetFoodItemId\":0}"))
			.andExpect(status().isBadRequest());

		verify(foodModerationService, never()).mergeFoodItem(eq(6L), anyLong());
	}

	private FoodModerationItem moderationItem(Long id, String name, int reportCount, String auditStatus) {
		return new FoodModerationItem(
			id,
			name,
			"dish",
			"snack",
			null,
			null,
			null,
			"user_generated",
			auditStatus,
			reportCount,
			2L,
			OffsetDateTime.parse("2026-06-14T12:00:00Z"),
			OffsetDateTime.parse("2026-06-14T12:00:00Z")
		);
	}
}
