package com.snk.server.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TokenCipher {
	private static final SecureRandom RANDOM = new SecureRandom();
	private final AuthProperties properties;

	public TokenCipher(AuthProperties properties) { this.properties = properties; }

	public String encrypt(String plaintext, String aad) { return crypt(Cipher.ENCRYPT_MODE, plaintext, aad); }
	public String decrypt(String ciphertext, String aad) { return crypt(Cipher.DECRYPT_MODE, ciphertext, aad); }

	private String crypt(int mode, String value, String aad) {
		try {
			byte[] key = Base64.getDecoder().decode(properties.tokenEncryptionKey());
			if (key.length != 32) throw new IllegalStateException("Token encryption key must be 256 bits");
			byte[] packed;
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			if (mode == Cipher.ENCRYPT_MODE) {
				byte[] nonce = new byte[12]; RANDOM.nextBytes(nonce);
				cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
				cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
				byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
				packed = new byte[nonce.length + encrypted.length];
				System.arraycopy(nonce, 0, packed, 0, nonce.length);
				System.arraycopy(encrypted, 0, packed, nonce.length, encrypted.length);
				return Base64.getUrlEncoder().withoutPadding().encodeToString(packed);
			}
			packed = Base64.getUrlDecoder().decode(value);
			byte[] nonce = java.util.Arrays.copyOfRange(packed, 0, 12);
			cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
			cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
			return new String(cipher.doFinal(packed, 12, packed.length - 12), StandardCharsets.UTF_8);
		}
		catch (Exception exception) { throw new IllegalStateException("Refresh token encryption failed", exception); }
	}
}
