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
import java.math.BigDecimal;
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
		when(foodSearchService.search(eq("lays")))
			.thenReturn(
				new FoodSearchResult(
					List.of(
						new FoodSearchItem(
							1L,
							"Lays Cucumber Chips",
							"packaged_product",
							"snack",
							"chips",
							"Lays",
							"6900000000011",
							"https://snk.qiuxinmin.cn/images/1.png",
							new BigDecimal("4.6"),
							"approved"
						)
					),
					"strong"
				)
			);

		mockMvc.perform(get("/api/foods/search").param("q", "lays"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.qualitySignal").value("strong"))
			.andExpect(jsonPath("$.items[0].name").value("Lays Cucumber Chips"))
			.andExpect(jsonPath("$.items[0].barcode").value("6900000000011"))
			.andExpect(jsonPath("$.items[0].coverImageUrl").value("https://snk.qiuxinmin.cn/images/1.png"))
			.andExpect(jsonPath("$.items[0].averageRating").value(4.6))
			.andExpect(jsonPath("$.items[0].auditStatus").value("approved"));
	}

	@Test
	void shouldReturnCreatorPendingFoodWhenUserIdIsProvided() throws Exception {
		when(foodSearchService.search(eq("mango cake"), eq(2L)))
			.thenReturn(
				new FoodSearchResult(
					List.of(
						new FoodSearchItem(
							9L,
							"Mango Cake",
							"packaged_product",
							"snack",
							"cake",
							"SNK Bakery",
							null,
							null,
							null,
							"pending"
						)
					),
					"strong"
				)
			);

		mockMvc.perform(get("/api/foods/search").param("q", "mango cake").param("userId", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.qualitySignal").value("strong"))
			.andExpect(jsonPath("$.items[0].name").value("Mango Cake"))
			.andExpect(jsonPath("$.items[0].auditStatus").value("pending"));
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
						"Lays Cucumber Chips",
						"packaged_product",
						"snack",
						"chips",
						"Lays",
						"6900000000011",
						null,
						null,
						"approved"
					)
				)
			);

		mockMvc.perform(get("/api/foods/barcode/6900000000011"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Lays Cucumber Chips"))
			.andExpect(jsonPath("$.barcode").value("6900000000011"))
			.andExpect(jsonPath("$.averageRating").isEmpty())
			.andExpect(jsonPath("$.auditStatus").value("approved"));
	}

	@Test
	void shouldReturnNotFoundWhenBarcodeMisses() throws Exception {
		when(foodSearchService.lookupByBarcode(eq("0000000000000"))).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/foods/barcode/0000000000000"))
			.andExpect(status().isNotFound());
	}

	@Test
	void shouldReturnRelatedFoods() throws Exception {
		when(foodSearchService.recommendRelatedFoods(1L, 5))
			.thenReturn(
				new FoodSearchResult(
					List.of(
						new FoodSearchItem(
							2L,
							"Lays Tomato Chips",
							"packaged_product",
							"snack",
							"chips",
							"Lays",
							"6900000000022",
							"https://snk.qiuxinmin.cn/images/2.png",
							new BigDecimal("4.4"),
							"approved"
						)
					),
					"related"
				)
			);

		mockMvc.perform(get("/api/foods/1/related"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.qualitySignal").value("related"))
			.andExpect(jsonPath("$.items[0].name").value("Lays Tomato Chips"))
			.andExpect(jsonPath("$.items[0].averageRating").value(4.4));
	}

	@Test
	void shouldCreatePendingFoodItem() throws Exception {
		when(
			manualFoodItemService.createPendingItem(
				eq(new CreateManualFoodItemCommand(2L, "Mango Cake", "packaged_product", "snack", "chips", "SNK Bakery", "6900000000099"))
			)
		).thenReturn(
			new FoodSearchItem(
				9L,
				"Mango Cake",
				"packaged_product",
				"snack",
				"chips",
				"SNK Bakery",
				"6900000000099",
				null,
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
					  "name": "Mango Cake",
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
			.andExpect(jsonPath("$.name").value("Mango Cake"))
			.andExpect(jsonPath("$.barcode").value("6900000000099"))
			.andExpect(jsonPath("$.auditStatus").value("pending"));
	}
}
