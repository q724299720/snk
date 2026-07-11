package com.snk.server.domain.auth;

import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;

public record RegistrationResult(
	Long userId,
	String username,
	AccountRole role,
	AccountStatus accountStatus,
	String approvalTicket
) {
}
