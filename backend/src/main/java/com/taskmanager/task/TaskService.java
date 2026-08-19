package com.taskmanager.task;

import org.springframework.stereotype.Service;

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

	public Task create(TaskFields fields) {
		requireNameNotTaken(fields.name());
		Instant now = clock.instant();
		Task task = Task.create(fields.name(), fields.service(), fields.description(), fields.status(),
				fields.script(), fields.cronExpr(), fields.maxExecutions(), fields.scheduled(), now);
		return repository.save(task);
	}

	public Task update(UUID id, TaskFields fields) {
		Task task = getById(id);
		requireNameNotTakenByAnotherTask(fields.name(), id);
		task.applyUpdate(fields.name(), fields.service(), fields.description(), fields.status(), fields.script(),
				fields.cronExpr(), fields.maxExecutions(), fields.scheduled(), clock.instant());
		return repository.save(task);
	}

	public Task getById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}

	public List<Task> listAll() {
		return repository.findAllByOrderByCreationDateDesc();
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
