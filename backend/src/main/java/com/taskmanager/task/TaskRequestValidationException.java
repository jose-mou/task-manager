package com.taskmanager.task;

import com.taskmanager.web.FieldError;

/**
 * Thrown when the {@code service} field of a task write is invalid given the
 * caller's identity: blank or missing for an ADMIN caller, or naming a
 * service that is not registered (specs/service-registry-and-task-scoping.md,
 * rules 9-10). Mapped to HTTP 400, naming {@code service}.
 *
 * Kept separate from Bean Validation because this rule depends on runtime
 * state - the authenticated caller and the service registry - that a
 * declarative constraint on {@link com.taskmanager.task.web.TaskRequest}
 * cannot see.
 */
public class TaskRequestValidationException extends RuntimeException {

	private final FieldError error;

	public TaskRequestValidationException(FieldError error) {
		super(error.field() + ": " + error.message());
		this.error = error;
	}

	public FieldError error() {
		return error;
	}

}
