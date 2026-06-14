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
			.thenReturn(List.of(foodItem("乐事黄瓜味薯片", "乐事", "6900000000011")));

		FoodSearchResult result = foodSearchService.search("乐事");

		assertThat(result.qualitySignal()).isEqualTo("strong");
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().name()).isEqualTo("乐事黄瓜味薯片");
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
			.thenReturn(Optional.of(foodItem("乐事黄瓜味薯片", "乐事", "6900000000011")));

		Optional<FoodSearchItem> result = foodSearchService.lookupByBarcode("6900000000011");

		assertThat(result).isPresent();
		assertThat(result.orElseThrow().barcode()).isEqualTo("6900000000011");
	}

	@Test
	void shouldReturnEmptyWhenBarcodeIsBlank() {
		Optional<FoodSearchItem> result = foodSearchService.lookupByBarcode(" ");

		assertThat(result).isEmpty();
	}

	private FoodItemEntity foodItem(String name, String brand, String barcode) {
		FoodItemEntity entity = new FoodItemEntity();
		TestFoodItemEntityAccessor.set(entity, name, brand, barcode);
		return entity;
	}

	private static final class TestFoodItemEntityAccessor {

		private TestFoodItemEntityAccessor() {
		}

		static void set(FoodItemEntity entity, String name, String brand, String barcode) {
			try {
				setField(entity, "name", name);
				setField(entity, "itemType", "packaged_product");
				setField(entity, "category", "snack");
				setField(entity, "subcategory", "chips");
				setField(entity, "brand", brand);
				setField(entity, "barcode", barcode);
				setField(entity, "auditStatus", "approved");
				setField(entity, "searchKeywords", name + " " + brand);
				setField(entity, "createdAt", OffsetDateTime.parse("2026-06-13T00:00:00Z"));
				setField(entity, "updatedAt", OffsetDateTime.parse("2026-06-13T00:00:00Z"));
			} catch (ReflectiveOperationException exception) {
				throw new IllegalStateException(exception);
			}
		}

		private static void setField(FoodItemEntity entity, String fieldName, Object value)
			throws ReflectiveOperationException {
			var field = FoodItemEntity.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(entity, value);
		}
	}
}
