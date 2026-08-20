package com.taskmanager.user;

import com.taskmanager.security.jwt.IssuedToken;
import com.taskmanager.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository repository;

	@Mock
	private JwtService jwtService;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private AuthService service() {
		return new AuthService(repository, passwordEncoder, jwtService);
	}

	@Test
	void loginIssuesATokenWhenThePasswordMatches() {
		User admin = User.create("admin", passwordEncoder.encode("admin"), UserRole.ADMIN, Instant.now());
		when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
		IssuedToken issued = new IssuedToken("token-value", Instant.now(), UserRole.ADMIN);
		when(jwtService.issue("admin", UserRole.ADMIN)).thenReturn(issued);

		assertThat(service().login("admin", "admin")).isEqualTo(issued);
	}

	@Test
	void loginThrowsInvalidCredentialsWhenThePasswordIsWrong() {
		User admin = User.create("admin", passwordEncoder.encode("admin"), UserRole.ADMIN, Instant.now());
		when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));

		assertThatThrownBy(() -> service().login("admin", "wrong-password"))
			.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void loginThrowsInvalidCredentialsWhenTheUsernameIsUnknown() {
		when(repository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().login("ghost", "whatever")).isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void aWrongPasswordAndAnUnknownUsernameFailIdentically() {
		User admin = User.create("admin", passwordEncoder.encode("admin"), UserRole.ADMIN, Instant.now());
		when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
		when(repository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());
		AuthService service = service();

		Throwable wrongPassword = catchThrowable(() -> service.login("admin", "wrong-password"));
		Throwable unknownUsername = catchThrowable(() -> service.login("ghost", "wrong-password"));

		assertThat(wrongPassword).hasSameClassAs(unknownUsername);
		assertThat(wrongPassword).hasMessage(unknownUsername.getMessage());
	}

	/**
	 * BCrypt is deliberately slow, so skipping it when the username is unknown would
	 * turn every login response time into a username oracle - and neither rate
	 * limiting nor account lockout is in scope to blunt it. Both branches must verify
	 * a password hash.
	 */
	@Test
	void anUnknownUsernameStillCostsAPasswordVerification() {
		PasswordEncoder countingEncoder = mock(PasswordEncoder.class);
		when(countingEncoder.encode(anyString())).thenReturn("$2a$10$placeholder");
		when(countingEncoder.matches(anyString(), anyString())).thenReturn(false);
		when(repository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

		AuthService service = new AuthService(repository, countingEncoder, jwtService);
		assertThatThrownBy(() -> service.login("ghost", "whatever")).isInstanceOf(InvalidCredentialsException.class);

		verify(countingEncoder).matches("whatever", "$2a$10$placeholder");
	}

}
