package com.snk.server.api.controller;

import com.snk.server.api.dto.AnonymousAuthRequest;
import com.snk.server.api.dto.AnonymousAuthResponse;
import com.snk.server.domain.user.AnonymousAuthService;
import com.snk.server.domain.user.AnonymousUserResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AnonymousAuthController {

	private final AnonymousAuthService anonymousAuthService;

	public AnonymousAuthController(AnonymousAuthService anonymousAuthService) {
		this.anonymousAuthService = anonymousAuthService;
	}

	@PostMapping("/anonymous")
	public AnonymousAuthResponse initializeAnonymousUser(@Valid @RequestBody AnonymousAuthRequest request) {
		AnonymousUserResult result = anonymousAuthService.initializeAnonymousUser(request.installationId());
		return new AnonymousAuthResponse(
			result.userId(),
			result.authProvider(),
			result.installationId(),
			result.newlyCreated(),
			result.createdAt(),
			result.lastSeenAt()
		);
	}
}
