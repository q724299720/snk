package com.snk.server.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.snk.server.domain.auth.RateLimitException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {
	private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofMinutes(1)).maximumSize(20_000).build();

	public void checkLogin(String ip, String username) { check("login:" + ip + ":" + username.toLowerCase(), 10); }
	public void checkRegistration(String ip) { check("register:" + ip, 5); }

	private void check(String key, int limit) {
		if (attempts.get(key, ignored -> new AtomicInteger()).incrementAndGet() > limit) {
			throw new RateLimitException(60);
		}
	}
}
