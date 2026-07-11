package com.snk.server.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.security.AuthProperties;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtTokenServiceTests {

	@Test
	void issuesAccessTokenWithAccountClaims() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair pair = generator.generateKeyPair();
		AuthProperties properties = new AuthProperties(Duration.ofMinutes(15), Duration.ofSeconds(60),
			pem("PRIVATE KEY", pair.getPrivate().getEncoded()), pem("PUBLIC KEY", pair.getPublic().getEncoded()),
			Base64.getEncoder().encodeToString(new byte[32]));
		JwtTokenService service = new JwtTokenService(properties);
		UserEntity user = new UserEntity();
		user.setUsername("owner");
		user.setRole(AccountRole.OWNER);
		user.setTokenVersion(3);

		String encoded = service.issue(user);
		Jwt jwt = service.decode(encoded);

		assertThat(jwt.getClaimAsString("username")).isEqualTo("owner");
		assertThat(jwt.getClaimAsString("role")).isEqualTo("OWNER");
		assertThat(((Number) jwt.getClaim("tokenVersion")).longValue()).isEqualTo(3L);
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
	}

	private static String pem(String type, byte[] encoded) {
		return "-----BEGIN " + type + "-----\n" + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded)
			+ "\n-----END " + type + "-----";
	}
}
