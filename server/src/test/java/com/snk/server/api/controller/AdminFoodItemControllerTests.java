package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.food.FoodModerationService;
import com.snk.server.domain.food.FoodModerationService.FoodModerationItem;
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
	void shouldApproveFoodItem() throws Exception {
		when(foodModerationService.approveFoodItem(3L))
			.thenReturn(moderationItem(3L, "Approved Item", 1, "approved"));

		mockMvc.perform(post("/api/admin/food-items/3/approve"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Approved Item"))
			.andExpect(jsonPath("$.auditStatus").value("approved"));
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
