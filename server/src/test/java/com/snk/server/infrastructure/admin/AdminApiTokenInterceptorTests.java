package com.snk.server.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminApiTokenInterceptorTests {

	@Test
	void preHandleAllowsRequestsWhenTokenIsNotConfigured() throws Exception {
		AdminProperties properties = new AdminProperties();
		AdminApiTokenInterceptor interceptor = new AdminApiTokenInterceptor(properties);

		boolean allowed = interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

		assertThat(allowed).isTrue();
	}

	@Test
	void preHandleRejectsRequestsWithoutTokenWhenConfigured() throws Exception {
		AdminProperties properties = new AdminProperties();
		properties.setApiToken("secret-token");
		AdminApiTokenInterceptor interceptor = new AdminApiTokenInterceptor(properties);
		MockHttpServletResponse response = new MockHttpServletResponse();

		boolean allowed = interceptor.preHandle(new MockHttpServletRequest(), response, new Object());

		assertThat(allowed).isFalse();
		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	void preHandleAllowsRequestsWithMatchingToken() throws Exception {
		AdminProperties properties = new AdminProperties();
		properties.setApiToken("secret-token");
		AdminApiTokenInterceptor interceptor = new AdminApiTokenInterceptor(properties);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-SNK-ADMIN-TOKEN", "secret-token");

		boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

		assertThat(allowed).isTrue();
	}
}
