package com.snk.server.domain.recognition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snk.server.domain.food.FoodSearchItem;
import com.snk.server.domain.food.FoodSearchResult;
import com.snk.server.domain.food.FoodSearchService;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.recognition.RecognitionTaskEntity;
import com.snk.server.infrastructure.persistence.recognition.RecognitionTaskRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecognitionTaskService {

	private static final TypeReference<List<ImageRecognitionCandidateSnapshot>> SNAPSHOT_LIST_TYPE = new TypeReference<>() {
	};

	private final RecognitionTaskRepository recognitionTaskRepository;
	private final UserRepository userRepository;
	private final FoodItemRepository foodItemRepository;
	private final ImageRecognitionTaskProvider imageRecognitionTaskProvider;
	private final FoodSearchService foodSearchService;
	private final ObjectMapper objectMapper;

	public RecognitionTaskService(
		RecognitionTaskRepository recognitionTaskRepository,
		UserRepository userRepository,
		FoodItemRepository foodItemRepository,
		ImageRecognitionTaskProvider imageRecognitionTaskProvider,
		FoodSearchService foodSearchService,
		ObjectMapper objectMapper
	) {
		this.recognitionTaskRepository = recognitionTaskRepository;
		this.userRepository = userRepository;
		this.foodItemRepository = foodItemRepository;
		this.imageRecognitionTaskProvider = imageRecognitionTaskProvider;
		this.foodSearchService = foodSearchService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public RecognitionTaskResult createTask(ImageRecognitionTaskCommand command) {
		String inputImageUrl = normalizeInputImageUrl(command.inputImageUrl());
		String hintQuery = normalizeOptional(command.hintQuery());
		UserEntity user = userRepository.findById(command.userId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

		RecognitionTaskEntity entity = new RecognitionTaskEntity();
		entity.setUser(user);
		entity.setInputImageUrl(inputImageUrl);
		entity.setStatus("processing");
		entity = recognitionTaskRepository.save(entity);

		try {
			ImageRecognitionTaskProviderResult providerResult = imageRecognitionTaskProvider.recognize(inputImageUrl);
			RecognitionTaskCompletion completion = resolveCompletion(hintQuery, providerResult);
			entity.setTopCandidates(writeSnapshots(completion.topCandidates()));
			entity.setConfidence(providerResult.confidence());
			entity.setSelectedFoodItem(
				completion.selectedFoodItemId() == null
					? null
					: foodItemRepository.findById(completion.selectedFoodItemId()).orElse(null)
			);
			entity.setStatus("completed");
			entity.setFinishedAt(java.time.OffsetDateTime.now());
			return toResult(recognitionTaskRepository.save(entity), null);
		} catch (ResponseStatusException exception) {
			entity.setStatus("failed");
			entity.setFinishedAt(java.time.OffsetDateTime.now());
			entity.setTopCandidates("[]");
			return toResult(recognitionTaskRepository.save(entity), exception.getReason());
		}
	}

	@Transactional(readOnly = true)
	public RecognitionTaskResult getTask(Long taskId) {
		RecognitionTaskEntity entity = recognitionTaskRepository.findById(taskId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recognition task not found."));
		return toResult(entity, null);
	}

	@Transactional(readOnly = true)
	public List<RecognitionTaskResult> listTasks(String status, Long userId, int limit) {
		int normalizedLimit = Math.min(Math.max(limit, 1), 100);
		var pageable = PageRequest.of(0, normalizedLimit);
		List<RecognitionTaskEntity> entities;
		String normalizedStatus = normalizeOptional(status);
		if (normalizedStatus != null && userId != null) {
			entities = recognitionTaskRepository
				.findByStatusAndUser_IdOrderByCreatedAtDesc(normalizedStatus, userId, pageable)
				.getContent();
		} else if (normalizedStatus != null) {
			entities = recognitionTaskRepository.findByStatusOrderByCreatedAtDesc(normalizedStatus, pageable).getContent();
		} else if (userId != null) {
			entities = recognitionTaskRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable).getContent();
		} else {
			entities = recognitionTaskRepository.findByOrderByCreatedAtDesc(pageable).getContent();
		}
		return entities.stream()
			.map(entity -> toResult(entity, null))
			.toList();
	}

	private String normalizeInputImageUrl(String inputImageUrl) {
		String normalized = inputImageUrl == null ? "" : inputImageUrl.trim();
		if (normalized.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inputImageUrl must not be blank");
		}
		return normalized;
	}

	private String normalizeOptional(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isBlank() ? null : normalized;
	}

	private RecognitionTaskCompletion resolveCompletion(String hintQuery, ImageRecognitionTaskProviderResult providerResult) {
		for (String query : buildCandidateQueries(hintQuery, providerResult.candidateQueries())) {
			FoodSearchResult searchResult = foodSearchService.search(query);
			if (!searchResult.items().isEmpty()) {
				List<ImageRecognitionCandidateSnapshot> snapshots = searchResult.items().stream()
					.limit(5)
					.map(this::toSnapshot)
					.toList();
				Long selectedFoodItemId = snapshots.isEmpty() ? null : snapshots.getFirst().foodItemId();
				return new RecognitionTaskCompletion(snapshots, selectedFoodItemId);
			}
		}
		return new RecognitionTaskCompletion(List.of(), null);
	}

	private List<String> buildCandidateQueries(String hintQuery, List<String> providerQueries) {
		LinkedHashSet<String> ordered = new LinkedHashSet<>();
		if (hintQuery != null) {
			ordered.add(hintQuery);
		}
		for (String providerQuery : providerQueries) {
			String normalized = normalizeOptional(providerQuery);
			if (normalized != null) {
				ordered.add(normalized);
			}
		}
		return new ArrayList<>(ordered);
	}

	private ImageRecognitionCandidateSnapshot toSnapshot(FoodSearchItem item) {
		return new ImageRecognitionCandidateSnapshot(
			item.id(),
			item.name(),
			item.itemType(),
			item.category(),
			item.subcategory(),
			item.brand(),
			item.barcode(),
			item.coverImageUrl(),
			item.auditStatus()
		);
	}

	private String writeSnapshots(List<ImageRecognitionCandidateSnapshot> snapshots) {
		try {
			return objectMapper.writeValueAsString(snapshots);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize recognition task candidates.", exception);
		}
	}

	private List<ImageRecognitionCandidateSnapshot> readSnapshots(String snapshotsJson) {
		try {
			return objectMapper.readValue(
				snapshotsJson == null || snapshotsJson.isBlank() ? "[]" : snapshotsJson,
				SNAPSHOT_LIST_TYPE
			);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to deserialize recognition task candidates.", exception);
		}
	}

	private RecognitionTaskResult toResult(RecognitionTaskEntity entity, String statusReason) {
		return new RecognitionTaskResult(
			entity.getId(),
			entity.getUser().getId(),
			entity.getInputImageUrl(),
			entity.getStatus(),
			readSnapshots(entity.getTopCandidates()),
			entity.getSelectedFoodItem() == null ? null : entity.getSelectedFoodItem().getId(),
			entity.getConfidence(),
			entity.getCreatedAt(),
			entity.getFinishedAt(),
			statusReason
		);
	}

	private record RecognitionTaskCompletion(
		List<ImageRecognitionCandidateSnapshot> topCandidates,
		Long selectedFoodItemId
	) {
	}
}
