package com.snk.server.domain.user;

import java.time.OffsetDateTime;

public record AnonymousUserResult(
	Long userId,
	String authProvider,
	String installationId,
	boolean newlyCreated,
	OffsetDateTime createdAt,
	OffsetDateTime lastSeenAt
) {
}
