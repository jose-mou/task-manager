package com.taskmanager.security;

import com.taskmanager.security.crypto.SecretHasher;
import com.taskmanager.service.ServiceAccount;
import com.taskmanager.service.ServiceAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test of the machine authentication of
 * specs/service-registry-and-task-scoping.md rule 3: HTTP Basic carrying the
 * {@code apiKey} as username and the {@code apiSecret} as password.
 */
@ExtendWith(MockitoExtension.class)
class ServiceCredentialsAuthenticationProviderTest {

	private static final String API_KEY = "RnyeHtZ5IOgN-oBcMGGmfiscmSFZdvA-dyPRge5EXyA";

	private static final String API_SECRET = "13DDBdX0UGRYiyKkg1Gm_XOMpE8Q4nzJZ4Ok70uHciY";

	@Mock
	private ServiceAccountRepository repository;

	private final SecretHasher secretHasher = new SecretHasher();

	private ServiceCredentialsAuthenticationProvider provider() {
		return new ServiceCredentialsAuthenticationProvider(repository, secretHasher);
	}

	private ServiceAccount registeredAccount() {
		return ServiceAccount.register("backup-service", API_KEY, secretHasher.hash(API_SECRET),
				Instant.parse("2026-08-19T10:15:00Z"));
	}

	private Authentication basicCredentials(String apiKey, String apiSecret) {
		return new UsernamePasswordAuthenticationToken(apiKey, apiSecret);
	}

	@Test
	void authenticatesTheMatchingApiKeyAndSecretAsAServicePrincipal() {
		ServiceAccount account = registeredAccount();
		when(repository.findByApiKey(API_KEY)).thenReturn(Optional.of(account));

		Authentication authenticated = provider().authenticate(basicCredentials(API_KEY, API_SECRET));

		assertThat(authenticated.getPrincipal())
			.isEqualTo(new ServicePrincipal(account.getId(), "backup-service"));
		assertThat(authenticated.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_SERVICE");
		assertThat(authenticated.isAuthenticated()).isTrue();
	}

	@Test
	void grantsNeitherTheAdminNorTheUserRole() {
		when(repository.findByApiKey(API_KEY)).thenReturn(Optional.of(registeredAccount()));

		Authentication authenticated = provider().authenticate(basicCredentials(API_KEY, API_SECRET));

		assertThat(authenticated.getAuthorities()).extracting(Object::toString)
			.doesNotContain("ROLE_ADMIN", "ROLE_USER");
	}

	@Test
	void rejectsAWrongApiSecretForAKnownApiKey() {
		when(repository.findByApiKey(API_KEY)).thenReturn(Optional.of(registeredAccount()));

		assertThatThrownBy(() -> provider().authenticate(basicCredentials(API_KEY, "not-the-secret")))
			.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void rejectsAnUnknownApiKey() {
		when(repository.findByApiKey("unknown-key")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> provider().authenticate(basicCredentials("unknown-key", API_SECRET)))
			.isInstanceOf(BadCredentialsException.class);
	}

	/**
	 * An unknown apiKey and a wrong apiSecret must be indistinguishable: the
	 * failure never reveals which half of the credentials was wrong, and never
	 * echoes the submitted secret (which would then reach the authentication
	 * failure logs).
	 */
	@Test
	void theFailureRevealsNeitherWhichHalfWasWrongNorTheSubmittedSecret() {
		when(repository.findByApiKey(API_KEY)).thenReturn(Optional.of(registeredAccount()));
		when(repository.findByApiKey("unknown-key")).thenReturn(Optional.empty());

		String wrongSecretMessage = messageOfFailure(API_KEY, "a-wrong-but-well-formed-secret");
		String unknownKeyMessage = messageOfFailure("unknown-key", API_SECRET);

		assertThat(wrongSecretMessage).isEqualTo(unknownKeyMessage);
		assertThat(wrongSecretMessage).doesNotContain("a-wrong-but-well-formed-secret").doesNotContain(API_KEY);
	}

	private String messageOfFailure(String apiKey, String apiSecret) {
		try {
			provider().authenticate(basicCredentials(apiKey, apiSecret));
			throw new AssertionError("Expected the credentials to be rejected");
		}
		catch (BadCredentialsException expected) {
			return expected.getMessage();
		}
	}

	@Test
	void supportsTheTokenTypeTheBasicAuthenticationFilterProduces() {
		assertThat(provider().supports(UsernamePasswordAuthenticationToken.class)).isTrue();
	}

}
