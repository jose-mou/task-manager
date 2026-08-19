package com.taskmanager.task.web;

import com.taskmanager.task.Task;
import com.taskmanager.task.TaskFields;
import com.taskmanager.task.TaskService;
import com.taskmanager.task.TaskStatus;
import com.taskmanager.task.TaskValidationException;
import com.taskmanager.task.TaskValidationException.FieldValidationError;
import com.taskmanager.task.validation.TaskRequestValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Thin REST layer for the task catalogue, delegating every rule to {@link TaskService}. */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskService taskService;

	private final TaskRequestValidator taskRequestValidator;

	public TaskController(TaskService taskService, TaskRequestValidator taskRequestValidator) {
		this.taskService = taskService;
		this.taskRequestValidator = taskRequestValidator;
	}

	@PostMapping
	public ResponseEntity<TaskResponse> create(@RequestBody TaskRequest request) {
		validate(request);
		Task task = taskService.create(toFields(request));
		return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TaskResponse> update(@PathVariable UUID id, @RequestBody TaskRequest request) {
		validate(request);
		Task task = taskService.update(id, toFields(request));
		return ResponseEntity.ok(TaskResponse.from(task));
	}

	@GetMapping("/{id}")
	public TaskResponse get(@PathVariable UUID id) {
		return TaskResponse.from(taskService.getById(id));
	}

	@GetMapping
	public List<TaskResponse> list() {
		return taskService.listAll().stream().map(TaskResponse::from).toList();
	}

	private void validate(TaskRequest request) {
		List<FieldValidationError> errors = taskRequestValidator.validate(request);
		if (!errors.isEmpty()) {
			throw new TaskValidationException(errors);
		}
	}

	private TaskFields toFields(TaskRequest request) {
		TaskStatus status = request.status() != null ? TaskStatus.valueOf(request.status()) : TaskStatus.CREATED;
		boolean scheduled = Boolean.TRUE.equals(request.scheduled());
		return new TaskFields(request.name(), request.service(), request.description(), status, request.script(),
				request.cronExpr(), request.maxExecutions(), scheduled);
	}

}
