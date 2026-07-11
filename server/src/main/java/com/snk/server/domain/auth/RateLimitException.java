package com.snk.server.domain.auth;

public class RateLimitException extends RuntimeException {
	private final int retryAfterSeconds;
	public RateLimitException(int retryAfterSeconds) { super("AUTH_RATE_LIMITED"); this.retryAfterSeconds = retryAfterSeconds; }
	public int retryAfterSeconds() { return retryAfterSeconds; }
}
