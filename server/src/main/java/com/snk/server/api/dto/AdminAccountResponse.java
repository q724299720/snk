package com.snk.server.api.dto;

import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import java.time.OffsetDateTime;

public record AdminAccountResponse(Long id, String username, AccountRole role, AccountStatus accountStatus,
	boolean mustChangePassword, OffsetDateTime approvedAt) {
	public static AdminAccountResponse from(UserEntity user) {
		return new AdminAccountResponse(user.getId(), user.getUsername(), user.getRole(), user.getAccountStatus(),
			user.isMustChangePassword(), user.getApprovedAt());
	}
}
