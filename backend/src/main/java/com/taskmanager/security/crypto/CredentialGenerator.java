package com.taskmanager.security.crypto;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates {@code apiKey}/{@code apiSecret} values: 256 random bits from a
 * CSPRNG, base64url-encoded without padding, matching the contract's
 * {@code ^[A-Za-z0-9_-]{43}$} pattern (api/openapi.yaml, ServiceCredentials).
 */
@Component
public class CredentialGenerator {

	private static final int KEY_LENGTH_BITS = 256;

	private final SecureRandom random = new SecureRandom();

	private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

	/** A fresh 256-bit value, base64url-encoded without padding. */
	public String generate() {
		byte[] bytes = new byte[KEY_LENGTH_BITS / 8];
		random.nextBytes(bytes);
		return encoder.encodeToString(bytes);
	}

}
