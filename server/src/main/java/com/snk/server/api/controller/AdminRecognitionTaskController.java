package com.snk.server.api.controller;

import com.snk.server.api.dto.RecognitionTaskResponse;
import com.snk.server.domain.recognition.RecognitionTaskService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/recognition-tasks")
public class AdminRecognitionTaskController {

	private final RecognitionTaskService recognitionTaskService;

	public AdminRecognitionTaskController(RecognitionTaskService recognitionTaskService) {
		this.recognitionTaskService = recognitionTaskService;
	}

	@GetMapping
	public List<RecognitionTaskResponse> listTasks(
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "userId", required = false) Long userId,
		@RequestParam(value = "limit", defaultValue = "20") int limit
	) {
		return recognitionTaskService.listTasks(status, userId, limit)
			.stream()
			.map(RecognitionTaskResponse::from)
			.toList();
	}

	@GetMapping("/{taskId}")
	public RecognitionTaskResponse getTask(@PathVariable("taskId") Long taskId) {
		return RecognitionTaskResponse.from(recognitionTaskService.getTask(taskId));
	}
}
