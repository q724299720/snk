package com.snk.server.api.dto;

import com.snk.server.infrastructure.persistence.user.AccountRole;

public record CurrentAccountResponse(Long userId, String username, AccountRole role, boolean mustChangePassword) {
}
