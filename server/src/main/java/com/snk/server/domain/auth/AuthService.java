package com.snk.server.domain.auth;

import com.snk.server.api.dto.TokenPairResponse;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
	private final UserRepository users;
	private final PasswordEncoder passwords;
	private final RefreshTokenService refreshTokens;

	public AuthService(UserRepository users, PasswordEncoder passwords, RefreshTokenService refreshTokens) {
		this.users = users; this.passwords = passwords; this.refreshTokens = refreshTokens;
	}

	@Transactional
	public TokenPairResponse login(String username, String password, String deviceId) {
		UserEntity user = users.findByUsernameIgnoreCase(username.trim())
			.orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "AUTH_CREDENTIALS_INVALID"));
		if (!passwords.matches(password, user.getPasswordHash())) throw error(HttpStatus.UNAUTHORIZED, "AUTH_CREDENTIALS_INVALID");
		if (user.getAccountStatus() != AccountStatus.ACTIVE) throw error(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_" + user.getAccountStatus().name());
		user.setLastLoginAt(OffsetDateTime.now()); users.save(user);
		return refreshTokens.create(user, deviceId);
	}

	public TokenPairResponse refresh(String refreshToken, String deviceId) { return refreshTokens.rotate(refreshToken, deviceId); }
	public void logout(Long userId, String deviceId) { refreshTokens.logout(userId, deviceId); }
	private static AuthException error(HttpStatus status, String code) { return new AuthException(status, code); }
}
