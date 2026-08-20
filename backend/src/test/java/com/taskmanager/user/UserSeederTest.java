package com.taskmanager.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Startup with no admin account is the one state that makes the whole feature
 * unusable, so a seeding failure must fail startup rather than boot silently
 * (specs/service-registry-and-task-scoping.md, rule 4).
 */
@ExtendWith(MockitoExtension.class)
class UserSeederTest {

	private static final Instant NOW = Instant.parse("2026-08-19T10:15:00Z");

	@Mock
	private UserRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

	private UserSeeder seeder() {
		return new UserSeeder(repository, passwordEncoder, clock, "admin", "admin");
	}

	@Test
	void seedsTheAdminAccountWhenTheUsersTableIsEmpty() {
		when(repository.count()).thenReturn(0L);
		when(passwordEncoder.encode("admin")).thenReturn("hashed-password");

		seeder().run(null);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(repository).save(captor.capture());
		User saved = captor.getValue();
		assertThat(saved.getUsername()).isEqualTo("admin");
		assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
		assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(saved.getCreationDate()).isEqualTo(NOW);
	}

	@Test
	void isANoOpWhenTheUsersTableIsAlreadyPopulated() {
		when(repository.count()).thenReturn(1L);

		seeder().run(null);

		verify(repository, never()).save(any());
	}

	@Test
	void aSeedingFailurePropagatesSoThatStartupFails() {
		when(repository.count()).thenReturn(0L);
		when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
		DataIntegrityViolationException cause = new DataIntegrityViolationException("duplicate key");
		when(repository.save(any())).thenThrow(cause);

		Throwable thrown = catchThrowable(() -> seeder().run(null));

		assertThat(thrown).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("admin")
			.hasMessageContaining("app.admin.username")
			.hasMessageContaining("app.admin.password");
		assertThat(thrown).cause().isSameAs(cause);
	}

}
