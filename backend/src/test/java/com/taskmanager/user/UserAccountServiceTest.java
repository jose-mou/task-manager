package com.taskmanager.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

	@Mock
	private UserRepository repository;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private UserAccountService service() {
		return new UserAccountService(repository, passwordEncoder);
	}

	private User adminWithPassword(String rawPassword) {
		return User.create("admin", passwordEncoder.encode(rawPassword), UserRole.ADMIN, Instant.now());
	}

	@Test
	void changePasswordUpdatesTheHashWhenEverythingIsValid() {
		User admin = adminWithPassword("admin");
		when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));

		service().changePassword("admin", "admin", "a-brand-new-valid-password");

		assertThat(passwordEncoder.matches("a-brand-new-valid-password", admin.getPasswordHash())).isTrue();
		verify(repository).save(admin);
	}

	@Test
	void changePasswordThrowsNamingCurrentPasswordWhenItDoesNotMatch() {
		User admin = adminWithPassword("admin");
		when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));

		assertThatThrownBy(() -> service().changePassword("admin", "wrong-current-password", "a-valid-new-password"))
			.isInstanceOf(PasswordValidationException.class)
			.satisfies(ex -> assertThat(((PasswordValidationException) ex).error().field()).isEqualTo("currentPassword"));

		verify(repository, never()).save(any());
	}

	@Test
	void changePasswordThrowsNamingNewPasswordWhenItIsTooShort() {
		User admin = adminWithPassword("admin");
		when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));

		assertThatThrownBy(() -> service().changePassword("admin", "admin", "short"))
			.isInstanceOf(PasswordValidationException.class)
			.satisfies(ex -> assertThat(((PasswordValidationException) ex).error().field()).isEqualTo("newPassword"))
			.satisfies(ex -> assertThat(((PasswordValidationException) ex).error().message())
				.isEqualTo("must be at least 8 characters long"));

		verify(repository, never()).save(any());
	}

	@Test
	void changePasswordThrowsNamingNewPasswordWhenItEqualsTheCurrentOne() {
		String currentPassword = "current-password-long-enough";
		User admin = adminWithPassword(currentPassword);
		when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));

		assertThatThrownBy(() -> service().changePassword("admin", currentPassword, currentPassword))
			.isInstanceOf(PasswordValidationException.class)
			.satisfies(ex -> assertThat(((PasswordValidationException) ex).error().field()).isEqualTo("newPassword"))
			.satisfies(ex -> assertThat(((PasswordValidationException) ex).error().message())
				.isEqualTo("must be different from the current password"));

		verify(repository, never()).save(any());
	}

}
