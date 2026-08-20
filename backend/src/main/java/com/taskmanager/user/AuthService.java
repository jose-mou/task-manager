package com.taskmanager.user;

import com.taskmanager.security.jwt.IssuedToken;
import com.taskmanager.security.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Authenticates UI users and issues their JWT (specs/service-registry-and-task-scoping.md, rule 3). */
@Service
public class AuthService {

	private final UserRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	/**
	 * A well-formed BCrypt hash of a value nobody can submit, verified against
	 * whenever the username is unknown.
	 *
	 * The 401 "never distinguishes which of the two was wrong"
	 * (specs/service-registry-and-task-scoping.md rule 3, api/openapi.yaml), and
	 * how long the answer takes is part of what it tells. BCrypt is deliberately
	 * slow, so returning early for an unknown username would make every login
	 * response a username oracle - the more so because rate limiting and account
	 * lockout are explicitly out of scope. Hashing against this placeholder keeps
	 * both branches doing the same work.
	 */
	private final String unknownUserPasswordHash;

	public AuthService(UserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.unknownUserPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional(readOnly = true)
	public IssuedToken login(String username, String password) {
		User user = repository.findByUsernameIgnoreCase(username).orElse(null);
		String expectedHash = user != null ? user.getPasswordHash() : unknownUserPasswordHash;
		if (!passwordEncoder.matches(password, expectedHash) || user == null) {
			throw new InvalidCredentialsException();
		}
		return jwtService.issue(user.getUsername(), user.getRole());
	}

}
