package com.snk.server.infrastructure.security;

import com.snk.server.infrastructure.persistence.user.AccountRole;

public record AuthenticatedAccountPrincipal(Long userId, String username, AccountRole role) {
}
