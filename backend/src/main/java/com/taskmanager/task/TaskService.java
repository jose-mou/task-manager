package com.taskmanager.task;

import com.taskmanager.security.CallerIdentity;
import com.taskmanager.security.ServiceCaller;
import com.taskmanager.service.ServiceAccount;
import com.taskmanager.service.ServiceAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
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

	private final ServiceAccountRepository serviceAccountRepository;

	private final Clock clock;

	public TaskService(TaskRepository repository, ServiceAccountRepository serviceAccountRepository, Clock clock) {
		this.repository = repository;
		this.serviceAccountRepository = serviceAccountRepository;
		this.clock = clock;
	}

	@Transactional
	public Task create(TaskFields fields) {
		ServiceAccount owningService = requireExistingOwningService(fields.serviceId());
		requireNameNotTaken(fields.name(), fields.serviceId(), owningService.getName());
		Instant now = clock.instant();
		Task task = Task.create(fields.name(), owningService, fields.description(), fields.status(), fields.script(),
				fields.cronExpr(), fields.maxExecutions(), fields.scheduled(), now);
		return save(task, fields.name(), owningService.getName());
	}

	@Transactional
	public Task update(UUID id, TaskFields fields, CallerIdentity caller) {
		Task task = getById(id);
		requireOwnTaskWhenServiceCaller(task, caller);
		ServiceAccount owningService = requireExistingOwningService(fields.serviceId());
		requireNameNotTakenByAnotherTask(fields.name(), fields.serviceId(), owningService.getName(), id);
		task.applyUpdate(fields.name(), owningService, fields.description(), fields.status(), fields.script(),
				fields.cronExpr(), fields.maxExecutions(), fields.scheduled(), clock.instant());
		return save(task, fields.name(), owningService.getName());
	}

	@Transactional(readOnly = true)
	public Task getById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public List<Task> listAll(String serviceNameFilter) {
		if (serviceNameFilter != null && !serviceNameFilter.isBlank()) {
			return repository.findByOwningService_NameOrderByCreationDateDesc(serviceNameFilter);
		}
		return repository.findAllByOrderByCreationDateDesc();
	}

	/**
	 * Enforces "updating a task owned by another service returns 403 with the
	 * task unchanged" (specs/service-registry-and-task-scoping.md, rule 9). An
	 * ADMIN caller may update - and even move - any task.
	 */
	private void requireOwnTaskWhenServiceCaller(Task task, CallerIdentity caller) {
		if (caller instanceof ServiceCaller serviceCaller
				&& !task.getOwningService().getId().equals(serviceCaller.serviceId())) {
			throw new AccessDeniedException("Access denied");
		}
	}

	/**
	 * The owning service id has already been validated to exist by whoever
	 * resolved it (an ADMIN caller's payload is checked against the registry,
	 * a service caller's id is its own, already-authenticated identity), so a
	 * missing service here is an invariant violation, not a client error.
	 */
	private ServiceAccount requireExistingOwningService(UUID serviceId) {
		return serviceAccountRepository.findById(serviceId)
			.orElseThrow(() -> new IllegalStateException("Owning service " + serviceId + " not found"));
	}

	/**
	 * Flushes the task inside the current transaction so that a unique-name
	 * violation surfaces here. The pre-check above cannot be atomic on its own:
	 * two concurrent requests can both pass it and let the
	 * {@code ux_tasks_service_id_name_ci} index reject the loser, which must
	 * still be answered with 409, never 500.
	 */
	private Task save(Task task, String name, String serviceName) {
		try {
			return repository.saveAndFlush(task);
		}
		catch (DataIntegrityViolationException ex) {
			if (isUniqueViolation(ex)) {
				throw new DuplicateTaskNameException(name, serviceName);
			}
			throw ex;
		}
	}

	/**
	 * {@code 23505} is the SQL state for a unique violation, and
	 * {@code ux_tasks_service_id_name_ci} is the only unique constraint on
	 * {@code tasks} besides the randomly generated primary key.
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

	private void requireNameNotTaken(String name, UUID serviceId, String serviceName) {
		if (repository.existsByNameIgnoreCaseAndOwningService_Id(name, serviceId)) {
			throw new DuplicateTaskNameException(name, serviceName);
		}
	}

	private void requireNameNotTakenByAnotherTask(String name, UUID serviceId, String serviceName, UUID id) {
		Optional<Task> existing = repository.findByNameIgnoreCaseAndOwningService_Id(name, serviceId);
		if (existing.isPresent() && !existing.get().getId().equals(id)) {
			throw new DuplicateTaskNameException(name, serviceName);
		}
	}

}
