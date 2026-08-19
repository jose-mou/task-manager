package com.taskmanager.task;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Business logic for creating, updating, fetching and listing tasks. */
@Service
public class TaskService {

	private final TaskRepository repository;

	private final Clock clock;

	public TaskService(TaskRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional
	public Task create(TaskFields fields) {
		requireNameNotTaken(fields.name());
		Instant now = clock.instant();
		Task task = Task.create(fields.name(), fields.service(), fields.description(), fields.status(),
				fields.script(), fields.cronExpr(), fields.maxExecutions(), fields.scheduled(), now);
		return save(task, fields.name());
	}

	@Transactional
	public Task update(UUID id, TaskFields fields) {
		Task task = getById(id);
		requireNameNotTakenByAnotherTask(fields.name(), id);
		task.applyUpdate(fields.name(), fields.service(), fields.description(), fields.status(), fields.script(),
				fields.cronExpr(), fields.maxExecutions(), fields.scheduled(), clock.instant());
		return save(task, fields.name());
	}

	@Transactional(readOnly = true)
	public Task getById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public List<Task> listAll() {
		return repository.findAllByOrderByCreationDateDesc();
	}

	/**
	 * Flushes the task inside the current transaction so that a unique-name
	 * violation surfaces here. The pre-check above cannot be atomic on its own:
	 * two concurrent requests can both pass it and let the {@code ux_tasks_name_ci}
	 * index reject the loser, which must still be answered with 409, never 500.
	 */
	private Task save(Task task, String name) {
		try {
			return repository.saveAndFlush(task);
		}
		catch (DataIntegrityViolationException ex) {
			if (isUniqueViolation(ex)) {
				throw new DuplicateTaskNameException(name);
			}
			throw ex;
		}
	}

	/**
	 * {@code 23505} is the SQL state for a unique violation, and
	 * {@code ux_tasks_name_ci} is the only unique constraint on {@code tasks}
	 * besides the randomly generated primary key.
	 */
	private boolean isUniqueViolation(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
				return true;
			}
			if (cause.getCause() == cause) {
				return false;
			}
		}
		return false;
	}

	private void requireNameNotTaken(String name) {
		if (repository.existsByNameIgnoreCase(name)) {
			throw new DuplicateTaskNameException(name);
		}
	}

	private void requireNameNotTakenByAnotherTask(String name, UUID id) {
		Optional<Task> existing = repository.findByNameIgnoreCase(name);
		if (existing.isPresent() && !existing.get().getId().equals(id)) {
			throw new DuplicateTaskNameException(name);
		}
	}

}
