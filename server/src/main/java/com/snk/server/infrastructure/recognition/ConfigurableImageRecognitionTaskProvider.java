package com.snk.server.infrastructure.recognition;

import com.snk.server.domain.recognition.ImageRecognitionTaskProvider;
import com.snk.server.domain.recognition.ImageRecognitionTaskProviderResult;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ConfigurableImageRecognitionTaskProvider implements ImageRecognitionTaskProvider {

	private final RecognitionProperties recognitionProperties;

	public ConfigurableImageRecognitionTaskProvider(RecognitionProperties recognitionProperties) {
		this.recognitionProperties = recognitionProperties;
	}

	@Override
	public ImageRecognitionTaskProviderResult recognize(String inputImageUrl) {
		String provider = recognitionProperties.getImage().getProvider();
		if ("stub".equalsIgnoreCase(provider)) {
			String stubQuery = recognitionProperties.getImage().getStubQuery();
			if (stubQuery == null || stubQuery.isBlank()) {
				return new ImageRecognitionTaskProviderResult(List.of(), BigDecimal.ZERO);
			}
			return new ImageRecognitionTaskProviderResult(List.of(stubQuery.trim()), new BigDecimal("0.8500"));
		}
		throw new ResponseStatusException(
			HttpStatus.SERVICE_UNAVAILABLE,
			"image recognition provider is not configured"
		);
	}
}
