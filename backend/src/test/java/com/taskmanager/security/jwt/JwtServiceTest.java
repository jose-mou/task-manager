package com.taskmanager.security.jwt;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.taskmanager.user.UserRole;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

	private static final String SECRET = Base64.getEncoder()
		.encodeToString("a-256-bit-or-longer-test-secret-value-for-hs256!".getBytes());

	private static final String DEVELOPMENT_SECRET = Base64.getEncoder()
		.encodeToString("the-committed-local-development-placeholder-key!!".getBytes());

	private final Instant fixedNow = Instant.parse("2026-08-19T10:15:00Z");

	private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

	private final JwtService jwtService = new JwtService(SECRET, "PT8H", DEVELOPMENT_SECRET, clock);

	@Test
	void issuesATokenExpiringEightHoursFromNow() {
		IssuedToken issued = jwtService.issue("admin", UserRole.ADMIN);

		assertThat(issued.token()).isNotBlank();
		assertThat(issued.role()).isEqualTo(UserRole.ADMIN);
		assertThat(issued.expiresAt()).isEqualTo(fixedNow.plusSeconds(8 * 3600));
	}

	@Test
	void parsingAFreshlyIssuedTokenReturnsTheSameUsernameAndRole() {
		IssuedToken issued = jwtService.issue("backup-admin", UserRole.ADMIN);

		Optional<ParsedToken> parsed = jwtService.parse(issued.token());

		assertThat(parsed).contains(new ParsedToken("backup-admin", UserRole.ADMIN));
	}

	@Test
	void parsingReturnsTheUserRoleWhenIssuedForAUser() {
		IssuedToken issued = jwtService.issue("plain-user", UserRole.USER);

		assertThat(jwtService.parse(issued.token())).contains(new ParsedToken("plain-user", UserRole.USER));
	}

	/**
	 * The committed development key would let anyone who can read the source forge
	 * an ADMIN token, so using it must be visible in the startup log rather than
	 * silent. A key that was actually configured must not raise the warning.
	 */
	@Test
	void warnsOnlyWhenSigningWithTheCommittedDevelopmentKey() {
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		Logger logger = (Logger) LoggerFactory.getLogger(JwtService.class);
		logger.addAppender(appender);
		try {
			new JwtService(SECRET, "PT8H", DEVELOPMENT_SECRET, clock);
			assertThat(appender.list).isEmpty();

			new JwtService(DEVELOPMENT_SECRET, "PT8H", DEVELOPMENT_SECRET, clock);
			assertThat(appender.list).singleElement()
				.satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN))
				.satisfies(event -> assertThat(event.getFormattedMessage()).contains("app.jwt.secret"));
			assertThat(appender.list).noneMatch(event -> event.getFormattedMessage().contains(DEVELOPMENT_SECRET));
		}
		finally {
			logger.detachAppender(appender);
		}
	}

	@Test
	void parsingAMalformedTokenReturnsEmpty() {
		assertThat(jwtService.parse("not-a-jwt-at-all")).isEmpty();
	}

	@Test
	void parsingATokenSignedWithADifferentSecretReturnsEmpty() {
		JwtService otherService = new JwtService(
				Base64.getEncoder().encodeToString("a-completely-different-256-bit-secret-value!".getBytes()), "PT8H",
				DEVELOPMENT_SECRET, clock);
		String tokenFromOtherSecret = otherService.issue("admin", UserRole.ADMIN).token();

		assertThat(jwtService.parse(tokenFromOtherSecret)).isEmpty();
	}

	/**
	 * The role that decides ADMIN authority travels inside the token, so a caller
	 * re-encoding the payload with {@code "role":"ADMIN"} while keeping the original
	 * signature must not be trusted: HS256 covers the payload, and the token is
	 * rejected outright rather than parsed with the tampered claims.
	 */
	@Test
	void parsingATokenWhoseRoleClaimWasTamperedWithReturnsEmpty() {
		String userToken = jwtService.issue("plain-user", UserRole.USER).token();
		String[] parts = userToken.split("\\.");
		String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
		String escalatedPayload = payload.replace("\"role\":\"USER\"", "\"role\":\"ADMIN\"");
		assertThat(escalatedPayload).isNotEqualTo(payload);
		String tampered = parts[0] + "."
				+ Base64.getUrlEncoder().withoutPadding().encodeToString(escalatedPayload.getBytes()) + "." + parts[2];

		assertThat(jwtService.parse(tampered)).isEmpty();
	}

	@Test
	void parsingAnUnsignedTokenReturnsEmpty() {
		String[] parts = jwtService.issue("admin", UserRole.ADMIN).token().split("\\.");
		String algNone = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString("{\"alg\":\"none\"}".getBytes());

		assertThat(jwtService.parse(algNone + "." + parts[1] + ".")).isEmpty();
	}

	@Test
	void parsingAnExpiredTokenReturnsEmpty() {
		Clock past = Clock.fixed(fixedNow.minusSeconds(9 * 3600), ZoneOffset.UTC);
		JwtService expiredIssuer = new JwtService(SECRET, "PT8H", DEVELOPMENT_SECRET, past);
		String expiredToken = expiredIssuer.issue("admin", UserRole.ADMIN).token();

		assertThat(jwtService.parse(expiredToken)).isEmpty();
	}

}
