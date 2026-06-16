package com.snk.server.api.controller;

import com.snk.server.api.dto.CreateRecognitionTaskRequest;
import com.snk.server.api.dto.FoodSearchItemResponse;
import com.snk.server.api.dto.OcrRecognitionResponse;
import com.snk.server.api.dto.RecognitionTaskResponse;
import com.snk.server.domain.recognition.ImageRecognitionTaskCommand;
import com.snk.server.domain.recognition.RecognitionTaskService;
import com.snk.server.domain.recognition.ServerOcrRecognitionResult;
import com.snk.server.domain.recognition.ServerOcrRecognitionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/recognition")
public class RecognitionController {

	private final ServerOcrRecognitionService serverOcrRecognitionService;
	private final RecognitionTaskService recognitionTaskService;

	public RecognitionController(
		ServerOcrRecognitionService serverOcrRecognitionService,
		RecognitionTaskService recognitionTaskService
	) {
		this.serverOcrRecognitionService = serverOcrRecognitionService;
		this.recognitionTaskService = recognitionTaskService;
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

	@PostMapping(path = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public RecognitionTaskResponse createRecognitionTask(@Valid @RequestBody CreateRecognitionTaskRequest request) {
		return RecognitionTaskResponse.from(
			recognitionTaskService.createTask(
				new ImageRecognitionTaskCommand(
					request.userId(),
					request.inputImageUrl(),
					request.hintQuery()
				)
			)
		);
	}

	@GetMapping(path = "/tasks/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public RecognitionTaskResponse getRecognitionTask(@PathVariable("taskId") Long taskId) {
		return RecognitionTaskResponse.from(recognitionTaskService.getTask(taskId));
	}
}
