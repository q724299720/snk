package com.snk.server.api.controller;

import com.snk.server.api.dto.RecognitionTaskResponse;
import com.snk.server.domain.recognition.RecognitionTaskService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/recognition-tasks")
@Validated
public class AdminRecognitionTaskController {

	private static final Set<String> ALLOWED_TASK_STATUSES = Set.of("pending", "processing", "completed", "failed");

	private final RecognitionTaskService recognitionTaskService;

	public AdminRecognitionTaskController(RecognitionTaskService recognitionTaskService) {
		this.recognitionTaskService = recognitionTaskService;
	}

	@GetMapping
	public List<RecognitionTaskResponse> listTasks(
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "userId", required = false) @Positive Long userId,
		@RequestParam(value = "limit", defaultValue = "20") @Positive int limit
	) {
		return recognitionTaskService.listTasks(validateStatus(status), userId, limit)
			.stream()
			.map(RecognitionTaskResponse::from)
			.toList();
	}

	@GetMapping("/{taskId}")
	public RecognitionTaskResponse getTask(@PathVariable("taskId") @Positive Long taskId) {
		return RecognitionTaskResponse.from(recognitionTaskService.getTask(taskId));
	}

	private String validateStatus(String status) {
		if (status == null) {
			return null;
		}
		String normalizedStatus = status.trim();
		if (normalizedStatus.isBlank()) {
			return null;
		}
		if (!ALLOWED_TASK_STATUSES.contains(normalizedStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid recognition task status.");
		}
		return normalizedStatus;
	}
}
