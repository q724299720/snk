package com.snk.server.domain.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.food.FoodSearchProjection;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FoodSearchServiceTests {

	@Mock
	private FoodItemRepository foodItemRepository;

	@InjectMocks
	private FoodSearchService foodSearchService;

	@Test
	void shouldReturnStrongQualityForPrefixMatch() {
		when(foodItemRepository.searchApproved(eq("lays")))
			.thenReturn(List.of(searchProjection(1L, "Lays Cucumber Chips", "Lays", "6900000000011", "approved", "4.6")));

		FoodSearchResult result = foodSearchService.search("lays");

		assertThat(result.qualitySignal()).isEqualTo("strong");
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().name()).isEqualTo("Lays Cucumber Chips");
		assertThat(result.items().getFirst().auditStatus()).isEqualTo("approved");
		assertThat(result.items().getFirst().averageRating()).isEqualByComparingTo("4.6");
	}

	@Test
	void shouldReturnWeakQualityWhenNoResultsFound() {
		when(foodItemRepository.searchApproved(eq("unknown snack"))).thenReturn(List.of());

		FoodSearchResult result = foodSearchService.search("unknown snack");

		assertThat(result.qualitySignal()).isEqualTo("weak");
		assertThat(result.items()).isEmpty();
	}

	@Test
	void shouldReturnOwnPendingFoodWhenSearchingWithCreatorUserId() {
		when(foodItemRepository.searchVisibleToUser(eq("mango cake"), eq(2L)))
			.thenReturn(List.of(searchProjection(9L, "Mango Cake", "SNK Bakery", null, "pending", null)));

		FoodSearchResult result = foodSearchService.search("mango cake", 2L);

		assertThat(result.qualitySignal()).isEqualTo("strong");
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().id()).isEqualTo(9L);
		assertThat(result.items().getFirst().auditStatus()).isEqualTo("pending");
		assertThat(result.items().getFirst().averageRating()).isNull();
	}

	@Test
	void shouldReturnApprovedFoodWhenBarcodeMatches() {
		when(foodItemRepository.findByAuditStatusAndBarcode("approved", "6900000000011"))
			.thenReturn(Optional.of(foodItem("Lays Cucumber Chips", "Lays", "6900000000011", "approved")));

		Optional<FoodSearchItem> result = foodSearchService.lookupByBarcode("6900000000011");

		assertThat(result).isPresent();
		assertThat(result.orElseThrow().barcode()).isEqualTo("6900000000011");
		assertThat(result.orElseThrow().auditStatus()).isEqualTo("approved");
		assertThat(result.orElseThrow().averageRating()).isNull();
	}

	@Test
	void shouldReturnEmptyWhenBarcodeIsBlank() {
		Optional<FoodSearchItem> result = foodSearchService.lookupByBarcode(" ");

		assertThat(result).isEmpty();
	}

	@Test
	void shouldRecommendRelatedFoodsFromSimilarFields() {
		FoodItemEntity seed = foodItem("Lays Cucumber Chips", "Lays", "6900000000011", "approved");
		setId(seed, 1L);
		FoodItemEntity related = foodItem("Lays Tomato Chips", "Lays", "6900000000022", "approved");
		setId(related, 2L);
		when(foodItemRepository.findById(1L)).thenReturn(Optional.of(seed));
		when(foodItemRepository.searchApproved("Lays"))
			.thenReturn(List.of(
				searchProjection(1L, "Lays Cucumber Chips", "Lays", "6900000000011", "approved", "4.6"),
				searchProjection(2L, "Lays Tomato Chips", "Lays", "6900000000022", "approved", "4.4")
			));
		when(foodItemRepository.searchApproved("chips"))
			.thenReturn(List.of(searchProjection(2L, "Lays Tomato Chips", "Lays", "6900000000022", "approved", "4.4")));
		when(foodItemRepository.searchApproved("snack"))
			.thenReturn(List.of(searchProjection(2L, "Lays Tomato Chips", "Lays", "6900000000022", "approved", "4.4")));
		when(foodItemRepository.searchApproved("Lays Cucumber Chips"))
			.thenReturn(List.of(searchProjection(1L, "Lays Cucumber Chips", "Lays", "6900000000011", "approved", "4.6")));

		FoodSearchResult result = foodSearchService.recommendRelatedFoods(1L, 5);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().name()).isEqualTo("Lays Tomato Chips");
		assertThat(result.items().getFirst().averageRating()).isEqualByComparingTo("4.4");
		assertThat(result.qualitySignal()).isEqualTo("related");
	}

	@Test
	void shouldRejectUnknownFoodWhenRecommendationSeedMissing() {
		when(foodItemRepository.findById(99L)).thenReturn(Optional.empty());

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> foodSearchService.recommendRelatedFoods(99L, 5))
			.isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
			.hasMessageContaining("404 NOT_FOUND");
	}

	private FoodItemEntity foodItem(String name, String brand, String barcode, String auditStatus) {
		FoodItemEntity entity = new FoodItemEntity();
		entity.setName(name);
		entity.setItemType("packaged_product");
		entity.setCategory("snack");
		entity.setSubcategory("chips");
		entity.setBrand(brand);
		entity.setBarcode(barcode);
		entity.setSource("system");
		entity.setAuditStatus(auditStatus);
		entity.setSearchKeywords(name + " " + brand);
		entity.setReportCount(0);
		entity.setCreatedAt(OffsetDateTime.parse("2026-06-13T00:00:00Z"));
		entity.setUpdatedAt(OffsetDateTime.parse("2026-06-13T00:00:00Z"));
		return entity;
	}

	private FoodSearchProjection searchProjection(
		Long id,
		String name,
		String brand,
		String barcode,
		String auditStatus,
		String averageRating
	) {
		return new FoodSearchProjection() {
			@Override
			public Long getId() {
				return id;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getItemType() {
				return "packaged_product";
			}

			@Override
			public String getCategory() {
				return "snack";
			}

			@Override
			public String getSubcategory() {
				return "chips";
			}

			@Override
			public String getBrand() {
				return brand;
			}

			@Override
			public String getBarcode() {
				return barcode;
			}

			@Override
			public String getCoverImageUrl() {
				return "https://snk.qiuxinmin.cn/images/" + id + ".png";
			}

			@Override
			public String getAuditStatus() {
				return auditStatus;
			}

			@Override
			public BigDecimal getAverageRating() {
				return averageRating == null ? null : new BigDecimal(averageRating);
			}
		};
	}

	private void setId(FoodItemEntity entity, Long id) {
		try {
			var field = FoodItemEntity.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
