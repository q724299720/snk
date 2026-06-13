package com.snk.server.infrastructure.storage;

public record StoredObject(
	String objectKey,
	String resourceUrl,
	String contentType,
	long size
) {
}
