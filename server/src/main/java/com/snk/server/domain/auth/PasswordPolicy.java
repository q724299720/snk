package com.snk.server.domain.auth;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class PasswordPolicy {

	private PasswordPolicy() {
	}

	static void validate(String password) {
		if (password == null || password.length() < 12) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least 12 characters.");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes.");
		}
	}
}
