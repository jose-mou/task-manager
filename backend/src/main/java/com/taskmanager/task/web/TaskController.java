package com.taskmanager.task.web;

import com.taskmanager.security.CallerIdentity;
import com.taskmanager.security.CallerIdentityResolver;
import com.taskmanager.task.Task;
import com.taskmanager.task.TaskFields;
import com.taskmanager.task.TaskOwnershipResolver;
import com.taskmanager.task.TaskService;
import com.taskmanager.task.TaskStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Thin REST layer for the task catalogue. Reads are anonymous; writes
 * resolve the caller's identity ({@link CallerIdentityResolver}) and, from
 * it, the task's owning service ({@link TaskOwnershipResolver}) before
 * delegating every other rule to {@link TaskService}.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskService taskService;

	private final TaskOwnershipResolver ownershipResolver;

	private final CallerIdentityResolver callerIdentityResolver;

	public TaskController(TaskService taskService, TaskOwnershipResolver ownershipResolver,
			CallerIdentityResolver callerIdentityResolver) {
		this.taskService = taskService;
		this.ownershipResolver = ownershipResolver;
		this.callerIdentityResolver = callerIdentityResolver;
	}

	@PostMapping
	public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request, Authentication authentication) {
		CallerIdentity caller = callerIdentityResolver.resolve(authentication);
		Task task = taskService.create(toFields(request, caller));
		return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TaskResponse> update(@PathVariable UUID id, @Valid @RequestBody TaskRequest request,
			Authentication authentication) {
		CallerIdentity caller = callerIdentityResolver.resolve(authentication);
		Task task = taskService.update(id, toFields(request, caller), caller);
		return ResponseEntity.ok(TaskResponse.from(task));
	}

	@GetMapping("/{id}")
	public TaskResponse get(@PathVariable UUID id) {
		return TaskResponse.from(taskService.getById(id));
	}

	@GetMapping
	public List<TaskResponse> list(@RequestParam(required = false) String service) {
		return taskService.listAll(service).stream().map(TaskResponse::from).toList();
	}

	private TaskFields toFields(TaskRequest request, CallerIdentity caller) {
		TaskStatus status = request.status() != null ? TaskStatus.valueOf(request.status()) : TaskStatus.CREATED;
		boolean scheduled = Boolean.TRUE.equals(request.scheduled());
		UUID serviceId = ownershipResolver.resolveOwningServiceId(request.service(), caller);
		return new TaskFields(request.name(), serviceId, request.description(), status, request.script(),
				request.cronExpr(), request.maxExecutions(), scheduled);
	}

}
