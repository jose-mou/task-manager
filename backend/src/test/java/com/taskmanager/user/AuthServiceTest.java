package com.taskmanager.user;

import com.taskmanager.security.jwt.IssuedToken;
import com.taskmanager.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

}
