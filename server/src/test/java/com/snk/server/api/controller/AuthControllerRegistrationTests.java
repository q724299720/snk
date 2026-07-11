package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.auth.AccountRegistrationService;
import com.snk.server.domain.auth.AuthService;
import com.snk.server.domain.auth.PasswordService;
import com.snk.server.domain.auth.RegistrationResult;
import com.snk.server.domain.auth.RegistrationStatusResult;
import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.security.CurrentUser;
import com.snk.server.infrastructure.security.AuthRateLimiter;
import com.snk.server.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(AuthControllerRegistrationTests.ControllerTestConfiguration.class)
@WebMvcTest(AuthController.class)
class AuthControllerRegistrationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AccountRegistrationService registrationService;

	@MockBean
	private PasswordService passwordService;

	@MockBean
	private CurrentUser currentUser;

	@MockBean
	private AuthService authService;

	@MockBean
	private AuthRateLimiter rateLimiter;

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void registerShouldReturnPendingAccountWithoutLoginToken() throws Exception {
		when(registrationService.register("Alice", "correct-horse-12")).thenReturn(
			new RegistrationResult(9L, "Alice", AccountRole.USER, AccountStatus.PENDING, "approval-ticket")
		);

		mockMvc.perform(post("/api/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"username":"Alice","password":"correct-horse-12"}
				"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.userId").value(9))
			.andExpect(jsonPath("$.accountStatus").value("PENDING"))
			.andExpect(jsonPath("$.approvalTicket").value("approval-ticket"))
			.andExpect(jsonPath("$.accessToken").doesNotExist())
			.andExpect(jsonPath("$.refreshToken").doesNotExist());
	}

	@Test
	void registerShouldRejectDeviceIdParameter() throws Exception {
		mockMvc.perform(post("/api/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"username":"Alice","password":"correct-horse-12","deviceId":"not-accepted"}
				"""))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(registrationService);
	}

	@Test
	void registrationStatusShouldOnlyReturnAccountStatus() throws Exception {
		when(registrationService.getRegistrationStatus("approval-ticket"))
			.thenReturn(new RegistrationStatusResult(AccountStatus.ACTIVE));

		mockMvc.perform(get("/api/auth/registration-status").param("ticket", "approval-ticket"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
			.andExpect(jsonPath("$.userId").doesNotExist());
	}

	@Test
	void passwordChangeShouldUseTrustedCurrentUserInsteadOfRequestUserId() throws Exception {
		when(currentUser.requiredUserId()).thenReturn(42L);

		mockMvc.perform(post("/api/auth/password/change")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"oldPassword":"old-password-12","newPassword":"new-password-12"}
				"""))
			.andExpect(status().isNoContent());

		verify(passwordService).changePassword(42L, "old-password-12", "new-password-12");
	}
}
