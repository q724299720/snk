package com.snk.server.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.snk.server.domain.auth.JwtTokenService;
import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

class BearerTokenFilterTests {
	@Test
	void rejectsTokenWhenDatabaseTokenVersionChanged() throws Exception {
		JwtTokenService tokens = org.mockito.Mockito.mock(JwtTokenService.class);
		UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
		Jwt jwt = Jwt.withTokenValue("access").header("alg", "RS256").subject("7")
			.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).claim("tokenVersion", 1L).build();
		UserEntity user = new UserEntity(); user.setTokenVersion(2); user.setRole(AccountRole.USER); user.setAccountStatus(AccountStatus.ACTIVE);
		when(tokens.decode("access")).thenReturn(jwt); when(users.findById(7L)).thenReturn(Optional.of(user));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/records/public");
		request.addHeader("Authorization", "Bearer access");
		MockHttpServletResponse response = new MockHttpServletResponse();

		new BearerTokenFilter(tokens, users).doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString()).contains("TOKEN_INVALID");
	}
}
