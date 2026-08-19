package com.taskmanager.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A stored task and its scheduling metadata. Persisted as a JPA entity;
 * kept as the single domain model for this feature rather than introducing a
 * separate persistence mapping layer, which would be disproportionate for a
 * single-entity CRUD feature of this size.
 */
@Entity
@Table(name = "tasks")
public class Task {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(name = "creation_date", nullable = false)
	private Instant creationDate;

	@Column(name = "modification_date", nullable = false)
	private Instant modificationDate;

	@Column(nullable = false)
	private String service;

	@Column
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskStatus status;

	@Column(nullable = false)
	private String script;

	@Column(name = "cron_expr")
	private String cronExpr;

	@Column(name = "max_executions")
	private Integer maxExecutions;

	@Column(nullable = false)
	private boolean scheduled;

	protected Task() {
		// required by JPA
	}

	private Task(UUID id, String name, Instant creationDate, Instant modificationDate, String service,
			String description, TaskStatus status, String script, String cronExpr, Integer maxExecutions,
			boolean scheduled) {
		this.id = id;
		this.name = name;
		this.creationDate = creationDate;
		this.modificationDate = modificationDate;
		this.service = service;
		this.description = description;
		this.status = status;
		this.script = script;
		this.cronExpr = cronExpr;
		this.maxExecutions = maxExecutions;
		this.scheduled = scheduled;
	}

	/** Creates a brand-new task: generates its id and sets creationDate == modificationDate. */
	public static Task create(String name, String service, String description, TaskStatus status, String script,
			String cronExpr, Integer maxExecutions, boolean scheduled, Instant now) {
		return new Task(UUID.randomUUID(), name, now, now, service, description, status, script, cronExpr,
				maxExecutions, scheduled);
	}

	/** Applies the given field values, preserving creationDate and bumping modificationDate. */
	public void applyUpdate(String name, String service, String description, TaskStatus status, String script,
			String cronExpr, Integer maxExecutions, boolean scheduled, Instant now) {
		this.name = name;
		this.service = service;
		this.description = description;
		this.status = status;
		this.script = script;
		this.cronExpr = cronExpr;
		this.maxExecutions = maxExecutions;
		this.scheduled = scheduled;
		this.modificationDate = now;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Instant getCreationDate() {
		return creationDate;
	}

	public Instant getModificationDate() {
		return modificationDate;
	}

	public String getService() {
		return service;
	}

	public String getDescription() {
		return description;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getScript() {
		return script;
	}

	public String getCronExpr() {
		return cronExpr;
	}

	public Integer getMaxExecutions() {
		return maxExecutions;
	}

	public boolean isScheduled() {
		return scheduled;
	}

}
