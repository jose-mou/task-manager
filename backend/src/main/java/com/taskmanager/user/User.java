package com.taskmanager.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A UI user, independent from the service registry. Column names match the
 * fixture expectations of TaskAnonymousAndRoleAccessAcceptanceTest, which
 * seeds a USER-role row directly with JDBC (there is no user-creation
 * endpoint: self-registration is out of scope of the spec).
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String username;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Column(name = "creation_date", nullable = false)
	private Instant creationDate;

	protected User() {
		// required by JPA
	}

	private User(UUID id, String username, String passwordHash, UserRole role, Instant creationDate) {
		this.id = id;
		this.username = username;
		this.passwordHash = passwordHash;
		this.role = role;
		this.creationDate = creationDate;
	}

	public static User create(String username, String passwordHash, UserRole role, Instant now) {
		return new User(UUID.randomUUID(), username, passwordHash, role, now);
	}

	public void changePasswordHash(String newPasswordHash) {
		this.passwordHash = newPasswordHash;
	}

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public Instant getCreationDate() {
		return creationDate;
	}

}
