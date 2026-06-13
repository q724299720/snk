package com.snk.server.api.dto;

public record UploadImageResponse(
	String objectKey,
	String resourceUrl,
	String contentType,
	long size
) {
}
