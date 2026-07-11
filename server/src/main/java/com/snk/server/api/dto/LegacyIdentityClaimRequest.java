package com.snk.server.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LegacyIdentityClaimRequest(@NotBlank @Size(max=128) String installationId) {
}
