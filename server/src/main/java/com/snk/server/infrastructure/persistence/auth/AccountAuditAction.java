package com.snk.server.infrastructure.persistence.auth;

public enum AccountAuditAction {
	APPROVE,
	REJECT,
	DISABLE,
	ENABLE,
	PROMOTE,
	DEMOTE,
	REVOKE_SESSIONS,
	RESET_PASSWORD,
	CHANGE_PASSWORD,
	OWNER_FORCE_RESET,
	LEGACY_CLAIM
}
