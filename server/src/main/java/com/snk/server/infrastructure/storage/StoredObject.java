package com.snk.server.infrastructure.storage;

public record StoredObject(
	String objectKey,
	String resourceUrl,
	String thumbnailObjectKey,
	String thumbnailUrl,
	String contentType,
	long size
) {
}
