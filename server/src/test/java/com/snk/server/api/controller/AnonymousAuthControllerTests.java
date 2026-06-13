package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.user.AnonymousAuthService;
import com.snk.server.domain.user.AnonymousUserResult;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnonymousAuthController.class)
class AnonymousAuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AnonymousAuthService anonymousAuthService;

	@Test
	void shouldInitializeAnonymousUser() throws Exception {
		when(anonymousAuthService.initializeAnonymousUser(eq("install-1"))).thenReturn(
			new AnonymousUserResult(
				1L,
				"anonymous",
				"install-1",
				true,
				OffsetDateTime.parse("2026-06-13T22:00:00Z"),
				OffsetDateTime.parse("2026-06-13T22:00:00Z")
			)
		);

		mockMvc.perform(
			post("/api/auth/anonymous")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"installationId":"install-1"}
					""")
		)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(1))
			.andExpect(jsonPath("$.authProvider").value("anonymous"))
			.andExpect(jsonPath("$.installationId").value("install-1"))
			.andExpect(jsonPath("$.newlyCreated").value(true));
	}

	@Test
	void shouldRejectBlankInstallationId() throws Exception {
		mockMvc.perform(
			post("/api/auth/anonymous")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"installationId":" "}
					""")
		)
			.andExpect(status().isBadRequest());
	}
}
