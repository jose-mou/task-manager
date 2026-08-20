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

	private final SecretKey key;

	private final Duration expiration;

	private final Clock clock;

	public JwtService(@Value("${app.jwt.secret}") String base64Secret, @Value("${app.jwt.expiration}") String expiration,
			@Value("${app.jwt.development-secret}") String developmentSecret, Clock clock) {
		byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
		// hmacShaKeyFor refuses anything below the 256 bits HS256 requires.
		this.key = Keys.hmacShaKeyFor(keyBytes);
		this.expiration = Duration.parse(expiration);
		this.clock = clock;
		warnIfSigningWithTheDevelopmentKey(base64Secret, developmentSecret);
	}

	/**
	 * The signing key is what makes an ADMIN token unforgeable, and the built-in
	 * development default is committed to the repository - anyone who can read it
	 * can mint an ADMIN JWT. Startup stays possible (the local stack and the tests
	 * rely on the default), but never silently.
	 */
	private void warnIfSigningWithTheDevelopmentKey(String base64Secret, String developmentSecret) {
		if (base64Secret.equals(developmentSecret)) {
			log.warn("app.jwt.secret is still the built-in development key: anyone with access to the source can "
					+ "forge an ADMIN token. Set the JWT_SECRET environment variable outside local development.");
		}
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
