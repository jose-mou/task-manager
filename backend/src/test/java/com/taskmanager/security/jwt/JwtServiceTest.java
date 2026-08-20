package com.taskmanager.security.jwt;

import com.taskmanager.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

	private static final String SECRET = Base64.getEncoder()
		.encodeToString("a-256-bit-or-longer-test-secret-value-for-hs256!".getBytes());

	private final Instant fixedNow = Instant.parse("2026-08-19T10:15:00Z");

	private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

	private final JwtService jwtService = new JwtService(SECRET, "PT8H", clock);

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

	@Test
	void parsingAMalformedTokenReturnsEmpty() {
		assertThat(jwtService.parse("not-a-jwt-at-all")).isEmpty();
	}

	@Test
	void parsingATokenSignedWithADifferentSecretReturnsEmpty() {
		JwtService otherService = new JwtService(
				Base64.getEncoder().encodeToString("a-completely-different-256-bit-secret-value!".getBytes()), "PT8H",
				clock);
		String tokenFromOtherSecret = otherService.issue("admin", UserRole.ADMIN).token();

		assertThat(jwtService.parse(tokenFromOtherSecret)).isEmpty();
	}

	@Test
	void parsingAnExpiredTokenReturnsEmpty() {
		Clock past = Clock.fixed(fixedNow.minusSeconds(9 * 3600), ZoneOffset.UTC);
		JwtService expiredIssuer = new JwtService(SECRET, "PT8H", past);
		String expiredToken = expiredIssuer.issue("admin", UserRole.ADMIN).token();

		assertThat(jwtService.parse(expiredToken)).isEmpty();
	}

}
