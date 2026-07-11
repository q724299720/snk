package com.snk.server.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "snk.auth")
public record AuthProperties(
	@DefaultValue("15m") Duration accessTokenTtl,
	@DefaultValue("60s") Duration refreshGracePeriod,
	String jwtPrivateKey,
	String jwtPublicKey,
	String tokenEncryptionKey
) {

	public AuthProperties {
		jwtPrivateKey = blankToNull(jwtPrivateKey);
		jwtPublicKey = blankToNull(jwtPublicKey);
		tokenEncryptionKey = blankToNull(tokenEncryptionKey);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
