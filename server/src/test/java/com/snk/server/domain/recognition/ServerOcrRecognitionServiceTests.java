package com.snk.server.domain.recognition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.snk.server.domain.food.FoodSearchItem;
import com.snk.server.domain.food.FoodSearchResult;
import com.snk.server.domain.food.FoodSearchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ServerOcrRecognitionServiceTests {

	@Mock
	private ServerOcrProvider serverOcrProvider;

	@Mock
	private FoodSearchService foodSearchService;

	@InjectMocks
	private ServerOcrRecognitionService serverOcrRecognitionService;

	@Test
	void shouldFallbackToMergedQueryWhenOriginalQueryMisses() {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"chips.png",
			"image/png",
			"png-content".getBytes()
		);
		when(serverOcrProvider.recognize(any(ServerOcrInput.class)))
			.thenReturn("Lays Cucumber Chips");
		when(foodSearchService.search(eq("Lays Cucumber Chips")))
			.thenReturn(new FoodSearchResult(List.of(), "weak"));
		when(foodSearchService.search(eq("LaysCucumberChips")))
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
							null,
							null,
							"approved"
						)
					),
					"strong"
				)
			);

		ServerOcrRecognitionResult result = serverOcrRecognitionService.recognize(file, "Lays Cucumber Chips");

		assertThat(result.recognizedText()).isEqualTo("Lays Cucumber Chips");
		assertThat(result.matchedQuery()).isEqualTo("LaysCucumberChips");
		assertThat(result.items()).hasSize(1);
		assertThat(result.qualitySignal()).isEqualTo("strong");
	}

	@Test
	void shouldReturnNoMatchWhenProviderReturnsBlankText() {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"blank.png",
			"image/png",
			"png-content".getBytes()
		);
		when(serverOcrProvider.recognize(any(ServerOcrInput.class)))
			.thenReturn("");

		ServerOcrRecognitionResult result = serverOcrRecognitionService.recognize(file, null);

		assertThat(result.recognizedText()).isEmpty();
		assertThat(result.items()).isEmpty();
		assertThat(result.attemptedQueries()).isEmpty();
		assertThat(result.qualitySignal()).isEqualTo("weak");
	}

	@Test
	void shouldRejectNonImageUpload() {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"bad.txt",
			"text/plain",
			"bad".getBytes()
		);

		assertThatThrownBy(() -> serverOcrRecognitionService.recognize(file, null))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("400 BAD_REQUEST");
	}
}
