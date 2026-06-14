package com.snk.server.domain.recognition;

import com.snk.server.domain.food.FoodSearchResult;
import com.snk.server.domain.food.FoodSearchService;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServerOcrRecognitionService {

	private final ServerOcrProvider serverOcrProvider;
	private final FoodSearchService foodSearchService;

	public ServerOcrRecognitionService(ServerOcrProvider serverOcrProvider, FoodSearchService foodSearchService) {
		this.serverOcrProvider = serverOcrProvider;
		this.foodSearchService = foodSearchService;
	}

	public ServerOcrRecognitionResult recognize(MultipartFile file, String clientRecognizedText) {
		validateImage(file);
		ServerOcrInput input = toInput(file, clientRecognizedText);
		String recognizedText = serverOcrProvider.recognize(input);
		List<String> attemptedQueries = ServerOcrSearchQueryBuilder.build(recognizedText);
		if (attemptedQueries.isEmpty()) {
			return new ServerOcrRecognitionResult(recognizedText, List.of(), null, "weak", List.of());
		}

		for (String query : attemptedQueries) {
			FoodSearchResult result = foodSearchService.search(query);
			if (!result.items().isEmpty()) {
				return new ServerOcrRecognitionResult(
					recognizedText,
					attemptedQueries,
					query,
					result.qualitySignal(),
					result.items()
				);
			}
		}

		return new ServerOcrRecognitionResult(recognizedText, attemptedQueries, null, "weak", List.of());
	}

	private void validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image file must not be empty");
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only image uploads are supported");
		}
	}

	private ServerOcrInput toInput(MultipartFile file, String clientRecognizedText) {
		try {
			return new ServerOcrInput(
				file.getBytes(),
				file.getOriginalFilename(),
				file.getContentType(),
				clientRecognizedText == null ? null : clientRecognizedText.trim()
			);
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to read uploaded image", exception);
		}
	}
}
