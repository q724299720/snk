package com.snk.server.api.controller;

import com.snk.server.api.dto.FoodSearchItemResponse;
import com.snk.server.api.dto.OcrRecognitionResponse;
import com.snk.server.domain.recognition.ServerOcrRecognitionResult;
import com.snk.server.domain.recognition.ServerOcrRecognitionService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/recognition")
public class RecognitionController {

	private final ServerOcrRecognitionService serverOcrRecognitionService;

	public RecognitionController(ServerOcrRecognitionService serverOcrRecognitionService) {
		this.serverOcrRecognitionService = serverOcrRecognitionService;
	}

	@PostMapping(
		path = "/ocr",
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public OcrRecognitionResponse recognizeByOcr(
		@RequestPart("file") MultipartFile file,
		@RequestPart(value = "clientRecognizedText", required = false) String clientRecognizedText
	) {
		ServerOcrRecognitionResult result = serverOcrRecognitionService.recognize(file, clientRecognizedText);
		List<FoodSearchItemResponse> items = result.items().stream()
			.map(FoodSearchItemResponse::from)
			.toList();
		return new OcrRecognitionResponse(
			result.recognizedText(),
			result.attemptedQueries(),
			result.matchedQuery(),
			result.qualitySignal(),
			items
		);
	}
}
