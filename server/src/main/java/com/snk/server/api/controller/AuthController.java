package com.snk.server.api.controller;

import com.snk.server.api.dto.PasswordChangeRequest;
import com.snk.server.api.dto.LoginRequest;
import com.snk.server.api.dto.LogoutRequest;
import com.snk.server.api.dto.RefreshRequest;
import com.snk.server.api.dto.TokenPairResponse;
import com.snk.server.api.dto.RegisterRequest;
import com.snk.server.api.dto.RegistrationResponse;
import com.snk.server.api.dto.RegistrationStatusResponse;
import com.snk.server.domain.auth.AccountRegistrationService;
import com.snk.server.domain.auth.AuthService;
import com.snk.server.domain.auth.PasswordService;
import com.snk.server.domain.auth.RegistrationResult;
import com.snk.server.infrastructure.security.CurrentUser;
import com.snk.server.infrastructure.security.AuthRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
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
	private final AuthService authService;
	private final AuthRateLimiter rateLimiter;

	public AuthController(
		AccountRegistrationService registrationService,
		PasswordService passwordService,
		CurrentUser currentUser,
		AuthService authService,
		AuthRateLimiter rateLimiter
	) {
		this.registrationService = registrationService;
		this.passwordService = passwordService;
		this.currentUser = currentUser;
		this.authService = authService;
		this.rateLimiter = rateLimiter;
	}

	@PostMapping("/login")
	public TokenPairResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		rateLimiter.checkLogin(httpRequest.getRemoteAddr(), request.username());
		return authService.login(request.username(), request.password(), request.deviceId());
	}

	@PostMapping("/refresh")
	public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request.refreshToken(), request.deviceId());
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(currentUser.requiredUserId(), request.deviceId());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/register")
	public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
		rateLimiter.checkRegistration(httpRequest.getRemoteAddr());
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
