package com.taskmanager.user;

import com.taskmanager.security.jwt.IssuedToken;
import com.taskmanager.security.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authenticates UI users and issues their JWT (specs/service-registry-and-task-scoping.md, rule 3). */
@Service
public class AuthService {

	private final UserRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	public AuthService(UserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional(readOnly = true)
	public IssuedToken login(String username, String password) {
		User user = repository.findByUsernameIgnoreCase(username).orElseThrow(InvalidCredentialsException::new);
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return jwtService.issue(user.getUsername(), user.getRole());
	}

}
