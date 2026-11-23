package com.zodiac.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Makes login tokens ("wristband codes") and their fingerprints (hashes). */
@Component
public class TokenService {

	/** 32 random bytes = 256 bits: far too many combinations to ever guess. */
	private static final int TOKEN_BYTES = 32;

	/** SecureRandom (not Random): unpredictable, safe for security purposes. */
	private final SecureRandom secureRandom = new SecureRandom();

	/** A new random token, as URL-safe text (43 characters). */
	public String generateToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * SHA-256 fingerprint of a token, as 64 hex characters. Same token → same hash, so we can look
	 * sessions up by it. Fast hashing is fine here (unlike passwords) because the token is already
	 * fully random, so there is nothing to "guess" from a dictionary.
	 */
	public String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is always available in Java", e);
		}
	}

}
