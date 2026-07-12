package com.snk.server.api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.auth.AccountAdministrationService;
import com.snk.server.infrastructure.security.CurrentUser;
import com.snk.server.infrastructure.storage.StorageProperties;
import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@Import(AdminAccountControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(AdminAccountController.class)
@TestPropertySource(properties = "snk.admin.api-token=test-admin-token")
class AdminAccountControllerTests {

	@Autowired private MockMvc mockMvc;
	@MockBean private AccountAdministrationService accounts;
	@MockBean private CurrentUser currentUser;

	@Test
	void approvesPendingAccountAsCurrentOwner() throws Exception {
		UserEntity pending = new UserEntity();
		pending.setUsername("pending-user");
		pending.setRole(AccountRole.USER);
		pending.setAccountStatus(AccountStatus.ACTIVE);
		when(currentUser.requiredUserId()).thenReturn(1L);
		when(accounts.approve(1L, 2L)).thenReturn(pending);

		mockMvc.perform(post("/api/admin/accounts/2/approve").header("X-SNK-ADMIN-TOKEN", "test-admin-token"))
			.andExpect(status().isOk());

		verify(accounts).approve(1L, 2L);
	}

	@TestConfiguration
	static class ControllerTestConfiguration {
		@Bean StorageProperties storageProperties() { return new StorageProperties(); }
	}
}
