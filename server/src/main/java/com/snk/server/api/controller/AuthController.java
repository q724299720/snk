package com.snk.server.api.controller;

import com.snk.server.api.dto.PasswordChangeRequest;
import com.snk.server.api.dto.RegisterRequest;
import com.snk.server.api.dto.RegistrationResponse;
import com.snk.server.api.dto.RegistrationStatusResponse;
import com.snk.server.domain.auth.AccountRegistrationService;
import com.snk.server.domain.auth.PasswordService;
import com.snk.server.domain.auth.RegistrationResult;
import com.snk.server.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AccountRegistrationService registrationService;
	private final PasswordService passwordService;
	private final CurrentUser currentUser;

	public AuthController(
		AccountRegistrationService registrationService,
		PasswordService passwordService,
		CurrentUser currentUser
	) {
		this.registrationService = registrationService;
		this.passwordService = passwordService;
		this.currentUser = currentUser;
	}

	@PostMapping("/register")
	public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
		RegistrationResult result = registrationService.register(request.username(), request.password());
		return ResponseEntity.status(HttpStatus.CREATED).body(new RegistrationResponse(
			result.userId(),
			result.username(),
			result.role(),
			result.accountStatus(),
			result.approvalTicket()
		));
	}

	@GetMapping("/registration-status")
	public RegistrationStatusResponse registrationStatus(@RequestParam @NotBlank String ticket) {
		return new RegistrationStatusResponse(registrationService.getRegistrationStatus(ticket).accountStatus());
	}

	@PostMapping("/password/change")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
		passwordService.changePassword(currentUser.requiredUserId(), request.oldPassword(), request.newPassword());
		return ResponseEntity.noContent().build();
	}
}
