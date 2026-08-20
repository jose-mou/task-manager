package com.taskmanager.task;

import com.taskmanager.service.ServiceAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

	/**
	 * EAGER (which is also the {@code @ManyToOne} default, stated explicitly
	 * here because the choice is deliberate): every {@code Task} response needs
	 * the owning service's name (api/openapi.yaml, {@code Task.service}), and
	 * {@code spring.jpa.open-in-view=false} means no Hibernate session is left
	 * open by the time {@link com.taskmanager.task.web.TaskResponse} is built in
	 * the web layer to initialize it lazily.
	 *
	 * Eager alone only join-fetches when loading by id; the list queries of
	 * {@link TaskRepository} carry an explicit entity graph so they do not
	 * degrade into one select per owner.
	 */
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "service_id", nullable = false)
	private ServiceAccount owningService;

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

	private Task(UUID id, String name, Instant creationDate, Instant modificationDate, ServiceAccount owningService,
			String description, TaskStatus status, String script, String cronExpr, Integer maxExecutions,
			boolean scheduled) {
		this.id = id;
		this.name = name;
		this.creationDate = creationDate;
		this.modificationDate = modificationDate;
		this.owningService = owningService;
		this.description = description;
		this.status = status;
		this.script = script;
		this.cronExpr = cronExpr;
		this.maxExecutions = maxExecutions;
		this.scheduled = scheduled;
	}

	/** Creates a brand-new task: generates its id and sets creationDate == modificationDate. */
	public static Task create(String name, ServiceAccount owningService, String description, TaskStatus status,
			String script, String cronExpr, Integer maxExecutions, boolean scheduled, Instant now) {
		return new Task(UUID.randomUUID(), name, now, now, owningService, description, status, script, cronExpr,
				maxExecutions, scheduled);
	}

	/** Applies the given field values, preserving creationDate and bumping modificationDate. */
	public void applyUpdate(String name, ServiceAccount owningService, String description, TaskStatus status,
			String script, String cronExpr, Integer maxExecutions, boolean scheduled, Instant now) {
		this.name = name;
		this.owningService = owningService;
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

	public ServiceAccount getOwningService() {
		return owningService;
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
