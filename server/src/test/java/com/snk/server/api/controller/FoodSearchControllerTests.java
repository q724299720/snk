package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.food.CreateManualFoodItemCommand;
import com.snk.server.domain.food.FoodSearchItem;
import com.snk.server.domain.food.FoodSearchResult;
import com.snk.server.domain.food.FoodSearchService;
import com.snk.server.domain.food.ManualFoodItemService;
import com.snk.server.infrastructure.storage.StorageProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Import(FoodSearchControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(FoodSearchController.class)
class FoodSearchControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FoodSearchService foodSearchService;

	@MockBean
	private ManualFoodItemService manualFoodItemService;

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
							null,
							"approved"
						)
					),
					"strong"
				)
			);

		mockMvc.perform(get("/api/foods/search").param("q", "乐事"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.qualitySignal").value("strong"))
			.andExpect(jsonPath("$.items[0].name").value("乐事黄瓜味薯片"))
			.andExpect(jsonPath("$.items[0].barcode").value("6900000000011"))
			.andExpect(jsonPath("$.items[0].auditStatus").value("approved"));
	}

	@Test
	void shouldRejectBlankQuery() throws Exception {
		mockMvc.perform(get("/api/foods/search").param("q", " "))
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnFoodWhenBarcodeMatches() throws Exception {
		when(foodSearchService.lookupByBarcode(eq("6900000000011")))
			.thenReturn(
				Optional.of(
					new FoodSearchItem(
						1L,
						"乐事黄瓜味薯片",
						"packaged_product",
						"snack",
						"chips",
						"乐事",
						"6900000000011",
						null,
						"approved"
					)
				)
			);

		mockMvc.perform(get("/api/foods/barcode/6900000000011"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("乐事黄瓜味薯片"))
			.andExpect(jsonPath("$.barcode").value("6900000000011"))
			.andExpect(jsonPath("$.auditStatus").value("approved"));
	}

	@Test
	void shouldReturnNotFoundWhenBarcodeMisses() throws Exception {
		when(foodSearchService.lookupByBarcode(eq("0000000000000"))).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/foods/barcode/0000000000000"))
			.andExpect(status().isNotFound());
	}

	@Test
	void shouldCreatePendingFoodItem() throws Exception {
		when(
			manualFoodItemService.createPendingItem(
				eq(new CreateManualFoodItemCommand(2L, "杨枝鲜花饼", "packaged_product", "snack", "chips", "SNK Bakery", "6900000000099"))
			)
		).thenReturn(
			new FoodSearchItem(
				9L,
				"杨枝鲜花饼",
				"packaged_product",
				"snack",
				"chips",
				"SNK Bakery",
				"6900000000099",
				null,
				"pending"
			)
		);

		mockMvc.perform(
			MockMvcRequestBuilders.post("/api/foods/manual")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "userId": 2,
					  "name": "杨枝鲜花饼",
					  "itemType": "packaged_product",
					  "category": "snack",
					  "subcategory": "chips",
					  "brand": "SNK Bakery",
					  "barcode": "6900000000099"
					}
					"""
				)
		)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("杨枝鲜花饼"))
			.andExpect(jsonPath("$.barcode").value("6900000000099"))
			.andExpect(jsonPath("$.auditStatus").value("pending"));
	}
}
