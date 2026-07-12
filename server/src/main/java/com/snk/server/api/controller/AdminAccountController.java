package com.snk.server.api.controller;

import com.snk.server.api.dto.AdminAccountResponse;
import com.snk.server.domain.auth.AccountAdministrationService;
import com.snk.server.infrastructure.security.CurrentUser;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
@Validated
public class AdminAccountController {
	private final AccountAdministrationService accounts;
	private final CurrentUser currentUser;
	public AdminAccountController(AccountAdministrationService accounts, CurrentUser currentUser) { this.accounts = accounts; this.currentUser = currentUser; }

	@GetMapping
	public List<AdminAccountResponse> list() { return accounts.listAccounts().stream().map(AdminAccountResponse::from).toList(); }
	@PostMapping("/{accountId}/approve") public AdminAccountResponse approve(@PathVariable @Positive Long accountId) { return AdminAccountResponse.from(accounts.approve(currentUser.requiredUserId(), accountId)); }
	@PostMapping("/{accountId}/reject") public AdminAccountResponse reject(@PathVariable @Positive Long accountId) { return AdminAccountResponse.from(accounts.reject(currentUser.requiredUserId(), accountId)); }
	@PostMapping("/{accountId}/disable") public AdminAccountResponse disable(@PathVariable @Positive Long accountId) { return AdminAccountResponse.from(accounts.disable(currentUser.requiredUserId(), accountId)); }
	@PostMapping("/{accountId}/enable") public AdminAccountResponse enable(@PathVariable @Positive Long accountId) { return AdminAccountResponse.from(accounts.enable(currentUser.requiredUserId(), accountId)); }
	@PostMapping("/{accountId}/promote") public AdminAccountResponse promote(@PathVariable @Positive Long accountId) { return AdminAccountResponse.from(accounts.promote(currentUser.requiredUserId(), accountId)); }
	@PostMapping("/{accountId}/demote") public AdminAccountResponse demote(@PathVariable @Positive Long accountId) { return AdminAccountResponse.from(accounts.demote(currentUser.requiredUserId(), accountId)); }
	@PostMapping("/{accountId}/revoke-sessions") public void revokeSessions(@PathVariable @Positive Long accountId) { accounts.revokeSessions(currentUser.requiredUserId(), accountId); }
	@PostMapping("/{accountId}/reset-password") public Map<String, String> resetPassword(@PathVariable @Positive Long accountId) { return Map.of("temporaryPassword", accounts.resetPassword(currentUser.requiredUserId(), accountId)); }
}
