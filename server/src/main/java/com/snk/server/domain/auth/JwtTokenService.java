package com.snk.server.domain.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.security.AuthProperties;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final AuthProperties properties;
	private volatile JwtEncoder encoder;
	private volatile JwtDecoder decoder;

	public JwtTokenService(AuthProperties properties) { this.properties = properties; }

	public String issue(UserEntity user) {
		ensureInitialized();
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("snk-server").issuedAt(now).expiresAt(now.plus(properties.accessTokenTtl()))
			.subject(String.valueOf(user.getId())).claim("username", user.getUsername())
			.claim("role", user.getRole().name()).claim("tokenVersion", user.getTokenVersion())
			.claim("mustChangePassword", user.isMustChangePassword()).build();
		return encoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(SignatureAlgorithm.RS256).keyId("snk-auth").build(), claims)).getTokenValue();
	}

	public Jwt decode(String token) { ensureInitialized(); return decoder.decode(token); }

	private synchronized void ensureInitialized() {
		if (encoder != null) return;
		try {
			if (properties.jwtPrivateKey() == null || properties.jwtPublicKey() == null) {
				throw new IllegalStateException("SNK JWT keys are not configured");
			}
			KeyFactory factory = KeyFactory.getInstance("RSA");
			RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(der(properties.jwtPrivateKey())));
			RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(der(properties.jwtPublicKey())));
			RSAKey key = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("snk-auth").build();
			encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
			decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
		}
		catch (Exception exception) { throw new IllegalStateException("Invalid SNK JWT key configuration", exception); }
	}

	private static byte[] der(String pem) {
		return Base64.getDecoder().decode(pem.replaceAll("-----BEGIN [^-]+-----", "")
			.replaceAll("-----END [^-]+-----", "").replaceAll("\\s", ""));
	}
}
