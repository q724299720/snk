package com.snk.server.domain.food;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
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
		when(foodItemRepository.searchApproved(eq("乐事")))
			.thenReturn(List.of(foodItem("乐事黄瓜味薯片", "乐事", "6900000000011", "approved")));

		FoodSearchResult result = foodSearchService.search("乐事");

		assertThat(result.qualitySignal()).isEqualTo("strong");
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().name()).isEqualTo("乐事黄瓜味薯片");
		assertThat(result.items().getFirst().auditStatus()).isEqualTo("approved");
	}

	@Test
	void shouldReturnWeakQualityWhenNoResultsFound() {
		when(foodItemRepository.searchApproved(eq("未知零食"))).thenReturn(List.of());

		FoodSearchResult result = foodSearchService.search("未知零食");

		assertThat(result.qualitySignal()).isEqualTo("weak");
		assertThat(result.items()).isEmpty();
	}

	@Test
	void shouldReturnApprovedFoodWhenBarcodeMatches() {
		when(foodItemRepository.findByAuditStatusAndBarcode("approved", "6900000000011"))
			.thenReturn(Optional.of(foodItem("乐事黄瓜味薯片", "乐事", "6900000000011", "approved")));

		Optional<FoodSearchItem> result = foodSearchService.lookupByBarcode("6900000000011");

		assertThat(result).isPresent();
		assertThat(result.orElseThrow().barcode()).isEqualTo("6900000000011");
		assertThat(result.orElseThrow().auditStatus()).isEqualTo("approved");
	}

	@Test
	void shouldReturnEmptyWhenBarcodeIsBlank() {
		Optional<FoodSearchItem> result = foodSearchService.lookupByBarcode(" ");

		assertThat(result).isEmpty();
	}

	@Test
	void shouldRecommendRelatedFoodsFromSimilarFields() {
		FoodItemEntity seed = foodItem("乐事黄瓜味薯片", "乐事", "6900000000011", "approved");
		setId(seed, 1L);
		FoodItemEntity related = foodItem("乐事番茄味薯片", "乐事", "6900000000022", "approved");
		setId(related, 2L);
		when(foodItemRepository.findById(1L)).thenReturn(Optional.of(seed));
		when(foodItemRepository.searchApproved("乐事"))
			.thenReturn(List.of(seed, related));
		when(foodItemRepository.searchApproved("chips"))
			.thenReturn(List.of(related));
		when(foodItemRepository.searchApproved("snack"))
			.thenReturn(List.of(related));
		when(foodItemRepository.searchApproved("乐事黄瓜味薯片"))
			.thenReturn(List.of(seed));

		FoodSearchResult result = foodSearchService.recommendRelatedFoods(1L, 5);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().name()).isEqualTo("乐事番茄味薯片");
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
