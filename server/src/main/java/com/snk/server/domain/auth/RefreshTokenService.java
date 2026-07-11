package com.snk.server.domain.auth;

import com.snk.server.api.dto.TokenPairResponse;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenEntity;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.security.AuthProperties;
import com.snk.server.infrastructure.security.TokenCipher;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
	private static final SecureRandom RANDOM = new SecureRandom();
	private final RefreshTokenRepository repository;
	private final JwtTokenService jwtTokens;
	private final TokenCipher cipher;
	private final AuthProperties properties;

	public RefreshTokenService(RefreshTokenRepository repository, JwtTokenService jwtTokens,
		TokenCipher cipher, AuthProperties properties) {
		this.repository = repository; this.jwtTokens = jwtTokens; this.cipher = cipher; this.properties = properties;
	}

	@Transactional
	public TokenPairResponse create(UserEntity user, String deviceId) {
		revoke(repository.findByUser_IdAndDeviceIdAndRevokedAtIsNull(user.getId(), deviceId));
		String raw = randomToken();
		RefreshTokenEntity entity = new RefreshTokenEntity();
		entity.setUser(user); entity.setDeviceId(deviceId); entity.setFamilyId(UUID.randomUUID());
		entity.setTokenHash(SecretHashing.sha256(raw));
		repository.save(entity);
		return pair(user, raw);
	}

	@Transactional
	public TokenPairResponse rotate(String rawToken, String deviceId) {
		RefreshTokenEntity old = repository.findByTokenHashForUpdate(SecretHashing.sha256(rawToken))
			.orElseThrow(() -> invalid("AUTH_REFRESH_INVALID"));
		OffsetDateTime now = OffsetDateTime.now();
		if (!old.getDeviceId().equals(deviceId)) {
			revokeFamily(old.getFamilyId(), now); throw invalid("AUTH_REFRESH_REPLAYED");
		}
		if (old.getRotatedAt() != null) {
			if (old.getReplacementExpiresAt() != null && !now.isAfter(old.getReplacementExpiresAt())) {
				return pair(old.getUser(), cipher.decrypt(old.getReplacementCiphertext(), aad(old)));
			}
			revokeFamily(old.getFamilyId(), now); throw invalid("AUTH_REFRESH_REPLAYED");
		}
		if (old.getRevokedAt() != null) throw invalid("AUTH_REFRESH_INVALID");
		String replacement = randomToken();
		RefreshTokenEntity next = new RefreshTokenEntity();
		next.setUser(old.getUser()); next.setDeviceId(deviceId); next.setFamilyId(old.getFamilyId());
		next.setTokenHash(SecretHashing.sha256(replacement)); repository.save(next);
		old.setRotatedAt(now); old.setRevokedAt(now);
		old.setReplacementExpiresAt(now.plus(properties.refreshGracePeriod()));
		old.setReplacementCiphertext(cipher.encrypt(replacement, aad(old)));
		repository.save(old);
		return pair(old.getUser(), replacement);
	}

	@Transactional
	public void logout(Long userId, String deviceId) {
		revoke(repository.findByUser_IdAndDeviceIdAndRevokedAtIsNull(userId, deviceId));
	}

	private TokenPairResponse pair(UserEntity user, String refresh) {
		return new TokenPairResponse(jwtTokens.issue(user), refresh, properties.accessTokenTtl().toSeconds());
	}

	private void revokeFamily(UUID familyId, OffsetDateTime now) {
		var active = repository.findByFamilyIdAndRevokedAtIsNull(familyId);
		active.forEach(token -> token.setRevokedAt(now)); repository.saveAll(active);
	}
	private void revoke(java.util.List<RefreshTokenEntity> tokens) {
		OffsetDateTime now = OffsetDateTime.now(); tokens.forEach(token -> token.setRevokedAt(now)); repository.saveAll(tokens);
	}
	private static String randomToken() { byte[] value = new byte[32]; RANDOM.nextBytes(value); return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
	private static String aad(RefreshTokenEntity token) { return token.getId() + ":" + token.getFamilyId() + ":" + token.getDeviceId(); }
	private static AuthException invalid(String code) { return new AuthException(HttpStatus.UNAUTHORIZED, code); }
}
