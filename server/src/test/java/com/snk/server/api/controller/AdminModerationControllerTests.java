package com.snk.server.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.food.FoodModerationAutoAuditService;
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

@Import(AdminModerationControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(AdminModerationController.class)
class AdminModerationControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FoodModerationAutoAuditService foodModerationAutoAuditService;

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void shouldRunAutoAuditOnDemand() throws Exception {
		when(foodModerationAutoAuditService.runAutoAudit())
			.thenReturn(new FoodModerationAutoAuditService.AutoAuditSummary(
				3,
				1,
				2,
				OffsetDateTime.parse("2026-06-20T12:00:00Z"),
				List.of(9L)
			));

		mockMvc.perform(post("/api/admin/moderation/auto-audit/run"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.scannedCount").value(3))
			.andExpect(jsonPath("$.rejectedCount").value(1))
			.andExpect(jsonPath("$.keptPendingCount").value(2))
			.andExpect(jsonPath("$.cutoffAt").value("2026-06-20T12:00:00Z"))
			.andExpect(jsonPath("$.rejectedFoodItemIds[0]").value(9));
	}
}
