package com.snk.server.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "snk.auth.owner")
public record OwnerBootstrapProperties(
	String username,
	String password,
	@DefaultValue("false") boolean forceReset
) {

	public OwnerBootstrapProperties {
		username = blankToNull(username);
		password = blankToNull(password);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
