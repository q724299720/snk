package com.snk.server.domain.recognition;

public record ServerOcrInput(
	byte[] content,
	String originalFilename,
	String contentType,
	String clientRecognizedText
) {
}
