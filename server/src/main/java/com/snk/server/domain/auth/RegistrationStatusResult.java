package com.snk.server.domain.auth;

import com.snk.server.infrastructure.persistence.user.AccountStatus;

public record RegistrationStatusResult(AccountStatus accountStatus) {
}
