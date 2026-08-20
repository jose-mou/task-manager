package com.taskmanager.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered machine identity that owns tasks and authenticates with
 * generated API credentials (specs/service-registry-and-task-scoping.md).
 * Named {@code ServiceAccount} rather than {@code Service} to avoid clashing
 * with {@link org.springframework.stereotype.Service}; the table itself is
 * {@code services}, as referenced by {@code Task.service}.
 */
@Entity
@Table(name = "services")
public class ServiceAccount {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	/** HTTP Basic username. Stored as-is: it is a public identifier, not a secret. */
	@Column(name = "api_key", nullable = false)
	private String apiKey;

	/** Hex-encoded SHA-256 hash of the apiSecret; the raw secret is never stored. */
	@Column(name = "api_secret_hash", nullable = false)
	private String apiSecretHash;

	@Column(name = "creation_date", nullable = false)
	private Instant creationDate;

	@Column(name = "modification_date", nullable = false)
	private Instant modificationDate;

	protected ServiceAccount() {
		// required by JPA
	}

	private ServiceAccount(UUID id, String name, String apiKey, String apiSecretHash, Instant creationDate,
			Instant modificationDate) {
		this.id = id;
		this.name = name;
		this.apiKey = apiKey;
		this.apiSecretHash = apiSecretHash;
		this.creationDate = creationDate;
		this.modificationDate = modificationDate;
	}

	public static ServiceAccount register(String name, String apiKey, String apiSecretHash, Instant now) {
		return new ServiceAccount(UUID.randomUUID(), name, apiKey, apiSecretHash, now, now);
	}

	public void rename(String name, Instant now) {
		this.name = name;
		this.modificationDate = now;
	}

	public void rotateCredentials(String apiKey, String apiSecretHash, Instant now) {
		this.apiKey = apiKey;
		this.apiSecretHash = apiSecretHash;
		this.modificationDate = now;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getApiSecretHash() {
		return apiSecretHash;
	}

	public Instant getCreationDate() {
		return creationDate;
	}

	public Instant getModificationDate() {
		return modificationDate;
	}

}
