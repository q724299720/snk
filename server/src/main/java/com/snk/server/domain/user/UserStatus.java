package com.snk.server.domain.user;

public enum UserStatus {
	ACTIVE("active"),
	DISABLED("disabled"),
	DELETED("deleted");

	private final String value;

	UserStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
