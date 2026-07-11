package com.snk.server.api.dto;

import com.snk.server.infrastructure.persistence.user.AccountStatus;

public record RegistrationStatusResponse(AccountStatus accountStatus) {
}
