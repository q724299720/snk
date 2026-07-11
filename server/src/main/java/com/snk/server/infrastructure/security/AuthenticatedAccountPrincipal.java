package com.snk.server.infrastructure.security;

import com.snk.server.infrastructure.persistence.user.AccountRole;

public record AuthenticatedAccountPrincipal(Long userId, String username, AccountRole role, long tokenVersion, boolean mustChangePassword) {
	public AuthenticatedAccountPrincipal(Long userId, String username, AccountRole role) {
		this(userId, username, role, 0, false);
	}
}
