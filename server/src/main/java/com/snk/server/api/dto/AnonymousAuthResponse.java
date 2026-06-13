package com.snk.server.api.dto;

import java.time.OffsetDateTime;

public record AnonymousAuthResponse(
	Long userId,
	String authProvider,
	String installationId,
	boolean newlyCreated,
	OffsetDateTime createdAt,
	OffsetDateTime lastSeenAt
) {
}
