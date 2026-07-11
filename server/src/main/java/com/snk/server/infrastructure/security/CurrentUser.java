package com.snk.server.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {

	public Long requiredUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
			&& authentication.isAuthenticated()
			&& authentication.getPrincipal() instanceof AuthenticatedAccountPrincipal principal) {
			return principal.userId();
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
	}
}
