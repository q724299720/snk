package com.snk.server.domain.user;

public enum AuthProvider {
	ANONYMOUS("anonymous"),
	PHONE("phone"),
	EMAIL("email"),
	WECHAT("wechat"),
	APPLE("apple"),
	GOOGLE("google");

	private final String value;

	AuthProvider(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
