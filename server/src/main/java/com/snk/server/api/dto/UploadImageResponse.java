package com.snk.server.api.dto;

public record UploadImageResponse(
	String objectKey,
	String resourceUrl,
	String thumbnailObjectKey,
	String thumbnailUrl,
	String contentType,
	long size
) {
}
