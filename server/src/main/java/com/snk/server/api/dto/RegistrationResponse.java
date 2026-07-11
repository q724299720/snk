package com.snk.server.api.dto;

import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;

public record RegistrationResponse(
	Long userId,
	String username,
	AccountRole role,
	AccountStatus accountStatus,
	String approvalTicket
) {
}
