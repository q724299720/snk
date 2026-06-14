package com.snk.server.domain.recognition;

public record ImageRecognitionTaskCommand(
	Long userId,
	String inputImageUrl
) {
}
