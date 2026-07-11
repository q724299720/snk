package com.snk.server.api.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public final class RegisterRequest {

	@NotBlank
	private final String username;

	@NotBlank
	private final String password;

	@JsonCreator
	public RegisterRequest(
		@JsonProperty("username") String username,
		@JsonProperty("password") String password
	) {
		this.username = username;
		this.password = password;
	}

	public String username() {
		return username;
	}

	public String password() {
		return password;
	}

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown registration field: " + fieldName);
	}
}
