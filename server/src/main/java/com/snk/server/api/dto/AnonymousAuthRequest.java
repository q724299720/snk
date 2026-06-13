package com.snk.server.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnonymousAuthRequest(
	@NotBlank(message = "installationId is required")
	@Size(max = 128, message = "installationId is too long")
	String installationId
) {
}
