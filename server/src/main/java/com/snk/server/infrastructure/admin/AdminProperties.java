package com.snk.server.infrastructure.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snk.admin")
public class AdminProperties {

	private String apiToken = "";

	private String tokenHeader = "X-SNK-ADMIN-TOKEN";

	public String getApiToken() {
		return apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	public String getTokenHeader() {
		return tokenHeader;
	}

	public void setTokenHeader(String tokenHeader) {
		this.tokenHeader = tokenHeader;
	}

	public boolean isApiTokenEnabled() {
		return apiToken != null && !apiToken.isBlank();
	}

	public String normalizedTokenHeader() {
		return tokenHeader == null || tokenHeader.isBlank() ? "X-SNK-ADMIN-TOKEN" : tokenHeader.trim();
	}
}
