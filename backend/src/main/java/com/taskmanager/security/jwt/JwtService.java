package com.taskmanager.security.jwt;

import com.taskmanager.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and parses the stateless HS256 JWT used by UI users
 * (specs/service-registry-and-task-scoping.md, rule 3): 8-hour expiry, no
 * refresh token, no revocation.
 */
@Component
public class JwtService {

	private static final Logger log = LoggerFactory.getLogger(JwtService.class);

	private static final String ROLE_CLAIM = "role";

	/** 256 bits, the minimum HS256 requires. */
	private static final int GENERATED_KEY_BYTES = 32;

	private final SecretKey key;

	private final Duration expiration;

	private final Clock clock;

	public JwtService(@Value("${app.jwt.secret}") String base64Secret, @Value("${app.jwt.expiration}") String expiration,
			Clock clock) {
		this.key = (base64Secret == null || base64Secret.isBlank()) ? generateEphemeralKey()
				: Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
		this.expiration = Duration.parse(expiration);
		this.clock = clock;
	}

	/**
	 * No signing key may ever be committed to the repository. When {@code
	 * app.jwt.secret} is absent or blank, a fresh 256-bit key is generated for
	 * this run instead of falling back to any constant. Tokens signed with it do
	 * not survive a restart, and it must never be shared across more than one
	 * instance - both consequences are surfaced here rather than silently, and
	 * the key itself (or any part of it) is never logged.
	 */
	private SecretKey generateEphemeralKey() {
		byte[] keyBytes = new byte[GENERATED_KEY_BYTES];
		new SecureRandom().nextBytes(keyBytes);
		log.warn("app.jwt.secret is not configured: generated a random signing key for this run. Issued tokens will "
				+ "not survive a restart and this setup must not be used with more than one instance. Set the "
				+ "JWT_SECRET environment variable to configure a persistent key.");
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public IssuedToken issue(String username, UserRole role) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(expiration);
		String token = Jwts.builder()
			.subject(username)
			.claim(ROLE_CLAIM, role.name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.signWith(key, Jwts.SIG.HS256)
			.compact();
		return new IssuedToken(token, expiresAt, role);
	}

	/**
	 * Empty when the token is missing, malformed, unsigned by this server, or
	 * expired. Expiration is checked against the injected {@link Clock} rather
	 * than the system clock, consistently with the rest of the application
	 * (see {@link com.taskmanager.config.ClockConfig}) and independently
	 * testable.
	 */
	public Optional<ParsedToken> parse(String token) {
		try {
			Claims claims = Jwts.parser()
				.clock(() -> Date.from(clock.instant()))
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
			String username = claims.getSubject();
			String roleValue = claims.get(ROLE_CLAIM, String.class);
			if (username == null || roleValue == null) {
				return Optional.empty();
			}
			return Optional.of(new ParsedToken(username, UserRole.valueOf(roleValue)));
		}
		catch (JwtException | IllegalArgumentException malformedOrExpired) {
			return Optional.empty();
		}
	}

}
