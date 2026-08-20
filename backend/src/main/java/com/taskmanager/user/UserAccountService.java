package com.taskmanager.user;

import com.taskmanager.web.FieldError;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Self-service password change (specs/service-registry-and-task-scoping.md, rule 5). */
@Service
public class UserAccountService {

	private static final int MIN_PASSWORD_LENGTH = 8;

	private final UserRepository repository;

	private final PasswordEncoder passwordEncoder;

	public UserAccountService(UserRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void changePassword(String username, String currentPassword, String newPassword) {
		User user = repository.findByUsernameIgnoreCase(username)
			.orElseThrow(() -> new IllegalStateException("Authenticated user '" + username + "' not found"));

		if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw new PasswordValidationException(new FieldError("currentPassword", "does not match the current password"));
		}
		if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
			throw new PasswordValidationException(
					new FieldError("newPassword", "must be at least " + MIN_PASSWORD_LENGTH + " characters long"));
		}
		if (newPassword.equals(currentPassword)) {
			throw new PasswordValidationException(
					new FieldError("newPassword", "must be different from the current password"));
		}

		user.changePasswordHash(passwordEncoder.encode(newPassword));
		repository.save(user);
	}

}
