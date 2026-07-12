package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.auth.JwtTokenService;
import com.snk.server.domain.record.FoodRecordResult;
import com.snk.server.domain.record.FoodRecordService;
import com.snk.server.domain.record.QuickFoodRecordCreateCommand;
import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import com.snk.server.infrastructure.security.CurrentUser;
import com.snk.server.infrastructure.security.SecurityConfiguration;
import com.snk.server.infrastructure.storage.ObjectStorageService;
import com.snk.server.infrastructure.storage.StorageProperties;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({FoodRecordController.class, UploadController.class})
@Import({SecurityConfiguration.class, CurrentUser.class, AuthenticatedBusinessApiTests.ControllerTestConfiguration.class})
@TestPropertySource(properties = "snk.auth.enforce-security=true")
class AuthenticatedBusinessApiTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FoodRecordService foodRecordService;
	@MockBean
	private JwtTokenService jwtTokens;
	@MockBean
	private UserRepository users;
	@MockBean
	private ObjectStorageService objectStorageService;
	@MockBean
	private com.snk.server.infrastructure.persistence.auth.UploadedObjectRepository uploadedObjects;

	@BeforeEach
	void authenticateTokenAccount() {
		UserEntity user = new UserEntity();
		user.setUsername("account-seven");
		user.setRole(AccountRole.USER);
		user.setAccountStatus(AccountStatus.ACTIVE);
		user.setTokenVersion(3L);
		when(jwtTokens.decode("test-access-token")).thenReturn(
			Jwt.withTokenValue("test-access-token")
				.header("alg", "none")
				.subject("7")
				.claim("tokenVersion", 3L)
				.build()
		);
		when(users.findById(7L)).thenReturn(Optional.of(user));
	}

	@Test
	void shouldRejectBusinessEndpointWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/api/records"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldRejectUploadWithoutBearerToken() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/upload/image")
				.file(new MockMultipartFile("file", "food.png", "image/png", new byte[] {1, 2, 3})))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldUseBearerAccountInsteadOfForgedRequestUserId() throws Exception {
		when(foodRecordService.createQuickRecord(any())).thenReturn(recordResult(7L));

		mockMvc.perform(post("/api/records/quick")
				.header(HttpHeaders.AUTHORIZATION, "Bearer test-access-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"clientRequestId":"b462a65b-b346-4a6d-bd87-c2022897544a","userId":999,"name":"Token owned food","isPublic":true,"rating":4}
					"""))
			.andExpect(status().isCreated());

		ArgumentCaptor<QuickFoodRecordCreateCommand> command = ArgumentCaptor.forClass(QuickFoodRecordCreateCommand.class);
		verify(foodRecordService).createQuickRecord(command.capture());
		org.assertj.core.api.Assertions.assertThat(command.getValue().userId()).isEqualTo(7L);
	}

	private FoodRecordResult recordResult(long userId) {
		return new FoodRecordResult(12L, userId, 4L, "manual", true, (short) 4, null, 0,
			OffsetDateTime.parse("2026-07-12T12:00:00Z"), OffsetDateTime.parse("2026-07-12T12:00:00Z"), List.of());
	}

	@TestConfiguration
	static class ControllerTestConfiguration {
		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}
}
