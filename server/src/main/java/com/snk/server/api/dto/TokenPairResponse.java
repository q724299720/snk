package com.snk.server.api.dto;

public record TokenPairResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

	public TokenPairResponse(String accessToken, String refreshToken, long expiresIn) {
		this(accessToken, refreshToken, "Bearer", expiresIn);
	}
}
