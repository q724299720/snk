package com.snk.server.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.snk.server.infrastructure.persistence.user.AccountRole;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

class CurrentUserTests {

	private final CurrentUser currentUser = new CurrentUser();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldReturnUserIdOnlyForAuthenticatedAccountPrincipal() {
		AuthenticatedAccountPrincipal principal = new AuthenticatedAccountPrincipal(42L, "alice", AccountRole.USER);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(principal, null, List.of())
		);

		assertThat(currentUser.requiredUserId()).isEqualTo(42L);
	}

	@Test
	void approvalTicketStringMustNotBecomeBusinessIdentity() {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("approval-ticket", null, List.of())
		);

		assertThatThrownBy(currentUser::requiredUserId)
			.isInstanceOf(ResponseStatusException.class)
			.extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
			.isEqualTo(401);
	}
}
