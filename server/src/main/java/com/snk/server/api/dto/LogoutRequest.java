package com.snk.server.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String deviceId) {
}
