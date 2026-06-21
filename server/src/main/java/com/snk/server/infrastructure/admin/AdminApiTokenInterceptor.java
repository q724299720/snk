package com.snk.server.infrastructure.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminApiTokenInterceptor implements HandlerInterceptor {

	private final AdminProperties adminProperties;

	public AdminApiTokenInterceptor(AdminProperties adminProperties) {
		this.adminProperties = adminProperties;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (!adminProperties.isApiTokenEnabled()) {
			return true;
		}

		String expectedToken = adminProperties.getApiToken().trim();
		String actualToken = request.getHeader(adminProperties.normalizedTokenHeader());
		if (matchesToken(expectedToken, actualToken)) {
			return true;
		}

		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "admin api token is required");
		return false;
	}

	private boolean matchesToken(String expectedToken, String actualToken) {
		if (actualToken == null || actualToken.isBlank()) {
			return false;
		}
		byte[] expectedBytes = expectedToken.getBytes(StandardCharsets.UTF_8);
		byte[] actualBytes = actualToken.trim().getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expectedBytes, actualBytes);
	}
}
