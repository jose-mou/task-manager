package com.taskmanager.service;

import com.taskmanager.security.AdminCaller;
import com.taskmanager.security.ServiceCaller;
import com.taskmanager.security.crypto.CredentialGenerator;
import com.taskmanager.security.crypto.SecretHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceAccountServiceTest {

	@Mock
	private ServiceAccountRepository repository;

	private final Instant fixedNow = Instant.parse("2026-08-19T10:15:30Z");

	private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

	private final CredentialGenerator credentialGenerator = new CredentialGenerator();

	private final SecretHasher secretHasher = new SecretHasher();

	private ServiceAccountService service() {
		return new ServiceAccountService(repository, credentialGenerator, secretHasher, clock);
	}

	@Test
	void registerGeneratesADistinctApiKeyAndApiSecretAndStoresOnlyTheSecretsHash() {
		when(repository.saveAndFlush(any(ServiceAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

		IssuedCredentials credentials = service().register("backup-service");

		assertThat(credentials.apiKey()).matches("^[A-Za-z0-9_-]{43}$");
		assertThat(credentials.apiSecret()).matches("^[A-Za-z0-9_-]{43}$");
		assertThat(credentials.apiKey()).isNotEqualTo(credentials.apiSecret());
		assertThat(credentials.name()).isEqualTo("backup-service");
	}

	@Test
	void registeredServiceCanBeAuthenticatedWithTheIssuedApiSecretAfterwards() {
		org.mockito.ArgumentCaptor<ServiceAccount> captor = org.mockito.ArgumentCaptor.forClass(ServiceAccount.class);
		when(repository.saveAndFlush(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

		IssuedCredentials credentials = service().register("backup-service");

		assertThat(secretHasher.matches(credentials.apiSecret(), captor.getValue().getApiSecretHash())).isTrue();
	}

	@Test
	void renameUpdatesTheNameAndModificationDateWhenCallerIsAdmin() {
		ServiceAccount account = ServiceAccount.register("old-name", "key", "hash", Instant.parse("2026-08-01T00:00:00Z"));
		when(repository.findById(account.getId())).thenReturn(Optional.of(account));
		when(repository.saveAndFlush(any(ServiceAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ServiceAccount renamed = service().rename(account.getId(), "new-name", new AdminCaller("admin"));

		assertThat(renamed.getName()).isEqualTo("new-name");
		assertThat(renamed.getModificationDate()).isEqualTo(fixedNow);
	}

	@Test
	void renameIsAllowedWhenTheServiceCallerRenamesItself() {
		ServiceAccount account = ServiceAccount.register("old-name", "key", "hash", Instant.parse("2026-08-01T00:00:00Z"));
		when(repository.findById(account.getId())).thenReturn(Optional.of(account));
		when(repository.saveAndFlush(any(ServiceAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ServiceCaller ownCaller = new ServiceCaller(account.getId(), "old-name");

		ServiceAccount renamed = service().rename(account.getId(), "new-name", ownCaller);

		assertThat(renamed.getName()).isEqualTo("new-name");
	}

	@Test
	void renameIsRejectedWhenAnotherServiceCallerAttemptsIt() {
		ServiceAccount account = ServiceAccount.register("old-name", "key", "hash", Instant.parse("2026-08-01T00:00:00Z"));
		when(repository.findById(account.getId())).thenReturn(Optional.of(account));
		ServiceCaller anotherCaller = new ServiceCaller(UUID.randomUUID(), "another-service");

		assertThatThrownBy(() -> service().rename(account.getId(), "new-name", anotherCaller))
			.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void rotateCredentialsReplacesBothValuesAndInvalidatesThePreviousSecret() {
		ServiceAccount account = ServiceAccount.register("svc", "old-key", secretHasher.hash("old-secret"),
				Instant.parse("2026-08-01T00:00:00Z"));
		when(repository.findById(account.getId())).thenReturn(Optional.of(account));
		when(repository.saveAndFlush(any(ServiceAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

		IssuedCredentials rotated = service().rotateCredentials(account.getId(), new AdminCaller("admin"));

		assertThat(rotated.apiKey()).isNotEqualTo("old-key");
		assertThat(rotated.apiSecret()).isNotEqualTo("old-secret");
		assertThat(secretHasher.matches("old-secret", account.getApiSecretHash())).isFalse();
		assertThat(secretHasher.matches(rotated.apiSecret(), account.getApiSecretHash())).isTrue();
	}

	@Test
	void deleteThrowsNotFoundWhenTheServiceDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.existsById(id)).thenReturn(false);

		assertThatThrownBy(() -> service().delete(id)).isInstanceOf(ServiceNotFoundException.class);

		verify(repository, never()).deleteById(any());
	}

	@Test
	void getByIdThrowsNotFoundWhenMissing() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().getById(id)).isInstanceOf(ServiceNotFoundException.class);
	}

}
