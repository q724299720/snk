package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.food.FoodSearchItem;
import com.snk.server.domain.recognition.ServerOcrRecognitionResult;
import com.snk.server.domain.recognition.ServerOcrRecognitionService;
import com.snk.server.infrastructure.storage.StorageProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Import(RecognitionControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(RecognitionController.class)
class RecognitionControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ServerOcrRecognitionService serverOcrRecognitionService;

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void shouldReturnServerOcrCandidates() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"chips.png",
			"image/png",
			"png-content".getBytes()
		);
		MockMultipartFile clientRecognizedText = new MockMultipartFile(
			"clientRecognizedText",
			"",
			"text/plain",
			"Lays Cucumber Chips".getBytes()
		);

		when(serverOcrRecognitionService.recognize(any(MultipartFile.class), any()))
			.thenReturn(
				new ServerOcrRecognitionResult(
					"Lays Cucumber Chips",
					List.of("Lays Cucumber Chips", "LaysCucumberChips"),
					"LaysCucumberChips",
					"strong",
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
							"approved"
						)
					)
				)
			);

		mockMvc.perform(multipart("/api/recognition/ocr").file(file).file(clientRecognizedText))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.recognizedText").value("Lays Cucumber Chips"))
			.andExpect(jsonPath("$.matchedQuery").value("LaysCucumberChips"))
			.andExpect(jsonPath("$.qualitySignal").value("strong"))
			.andExpect(jsonPath("$.items[0].name").value("Lays Cucumber Chips"));
	}

	@Test
	void shouldReturnServiceUnavailableWhenProviderIsDisabled() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"chips.png",
			"image/png",
			"png-content".getBytes()
		);

		when(serverOcrRecognitionService.recognize(any(MultipartFile.class), any()))
			.thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "server ocr provider is not configured"));

		mockMvc.perform(multipart("/api/recognition/ocr").file(file))
			.andExpect(status().isServiceUnavailable());
	}
}
