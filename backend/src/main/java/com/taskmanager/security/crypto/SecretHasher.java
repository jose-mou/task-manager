package com.taskmanager.security.crypto;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes and verifies service {@code apiSecret} values.
 *
 * specs/service-registry-and-task-scoping.md is explicit that the secret must
 * never be stored: only a hex-encoded SHA-256 hash is kept, verified with a
 * constant-time comparison. BCrypt is deliberately not used here (unlike user
 * passwords): apiSecret is already 256 random bits, so BCrypt's slow, salted
 * hashing defends against nothing extra and would only add unnecessary
 * latency to every service-authenticated request.
 */
@Component
public class SecretHasher {

	/** Hex-encodes the SHA-256 digest of {@code secret}, for storage. */
	public String hash(String secret) {
		return HexFormat.of().formatHex(digest(secret));
	}

	/** Constant-time comparison of {@code candidateSecret} against a stored {@link #hash(String)}. */
	public boolean matches(String candidateSecret, String storedHashHex) {
		if (candidateSecret == null || storedHashHex == null) {
			return false;
		}
		byte[] candidate = digest(candidateSecret);
		byte[] expected;
		try {
			expected = HexFormat.of().parseHex(storedHashHex);
		}
		catch (IllegalArgumentException malformedStoredHash) {
			return false;
		}
		return MessageDigest.isEqual(candidate, expected);
	}

	private byte[] digest(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException e) {
			// SHA-256 is guaranteed available on every standard JVM (JLS platform requirement).
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}

}
