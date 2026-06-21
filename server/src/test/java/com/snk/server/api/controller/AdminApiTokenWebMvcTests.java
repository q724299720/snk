package com.snk.server.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.admin.AdminStatsService;
import com.snk.server.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@Import(AdminApiTokenWebMvcTests.ControllerTestConfiguration.class)
@TestPropertySource(properties = "snk.admin.api-token=test-admin-token")
@WebMvcTest(AdminStatsController.class)
class AdminApiTokenWebMvcTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AdminStatsService adminStatsService;

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void shouldRejectAdminApiRequestWithoutConfiguredToken() throws Exception {
		mockMvc.perform(get("/api/admin/stats"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldAllowAdminApiRequestWithConfiguredToken() throws Exception {
		when(adminStatsService.getStats())
			.thenReturn(new AdminStatsService.AdminStatsResult(17, 3, 12, 2, 4, 10, 2, 1, 5, 2, 8, 1));

		mockMvc.perform(get("/api/admin/stats").header("X-SNK-ADMIN-TOKEN", "test-admin-token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalFoodItems").value(17));
	}
}
