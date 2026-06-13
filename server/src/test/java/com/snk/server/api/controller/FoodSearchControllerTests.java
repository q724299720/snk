package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.food.FoodSearchItem;
import com.snk.server.domain.food.FoodSearchResult;
import com.snk.server.domain.food.FoodSearchService;
import com.snk.server.infrastructure.storage.StorageProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(FoodSearchControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(FoodSearchController.class)
class FoodSearchControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FoodSearchService foodSearchService;

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void shouldReturnSearchResults() throws Exception {
		when(foodSearchService.search(eq("乐事")))
			.thenReturn(
				new FoodSearchResult(
					List.of(
						new FoodSearchItem(
							1L,
							"乐事黄瓜味薯片",
							"packaged_product",
							"snack",
							"chips",
							"乐事",
							"6900000000011",
							null
						)
					),
					"strong"
				)
			);

		mockMvc.perform(get("/api/foods/search").param("q", "乐事"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.qualitySignal").value("strong"))
			.andExpect(jsonPath("$.items[0].name").value("乐事黄瓜味薯片"))
			.andExpect(jsonPath("$.items[0].barcode").value("6900000000011"));
	}

	@Test
	void shouldRejectBlankQuery() throws Exception {
		mockMvc.perform(get("/api/foods/search").param("q", " "))
			.andExpect(status().isBadRequest());
	}
}
