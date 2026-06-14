package com.snk.server.infrastructure.recognition;

import com.snk.server.domain.recognition.ServerOcrInput;
import com.snk.server.domain.recognition.ServerOcrProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ConfigurableServerOcrProvider implements ServerOcrProvider {

	private final RecognitionProperties recognitionProperties;

	public ConfigurableServerOcrProvider(RecognitionProperties recognitionProperties) {
		this.recognitionProperties = recognitionProperties;
	}

	@Override
	public String recognize(ServerOcrInput input) {
		String provider = recognitionProperties.getOcr().getProvider();
		if ("stub".equalsIgnoreCase(provider)) {
			if (input.clientRecognizedText() != null && !input.clientRecognizedText().isBlank()) {
				return input.clientRecognizedText();
			}
			String stubText = recognitionProperties.getOcr().getStubText();
			return stubText == null ? "" : stubText.trim();
		}
		throw new ResponseStatusException(
			HttpStatus.SERVICE_UNAVAILABLE,
			"server ocr provider is not configured"
		);
	}
}
