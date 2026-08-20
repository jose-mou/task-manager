package com.taskmanager.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Seeds the ADMIN account on first startup, when the {@code users} table is
 * empty (specs/service-registry-and-task-scoping.md, rule 4). Username and
 * password are overridable through configuration (environment-injectable);
 * the defaults apply when unset. Seeding never fails startup: any error here
 * is logged and swallowed.
 */
@Component
public class UserSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

	private final UserRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final Clock clock;

	private final String adminUsername;

	private final String adminPassword;

	public UserSeeder(UserRepository repository, PasswordEncoder passwordEncoder, Clock clock,
			@Value("${app.admin.username}") String adminUsername, @Value("${app.admin.password}") String adminPassword) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			if (repository.count() != 0) {
				return;
			}
			User admin = User.create(adminUsername, passwordEncoder.encode(adminPassword), UserRole.ADMIN,
					clock.instant());
			repository.save(admin);
			log.info("Seeded the default ADMIN account '{}'", adminUsername);
		}
		catch (RuntimeException ex) {
			log.warn("Skipping ADMIN account seeding: {}", ex.getMessage());
		}
	}

}
