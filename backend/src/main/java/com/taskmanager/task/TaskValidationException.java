package com.taskmanager.task;

import java.util.List;

/**
 * Thrown when a task request fails validation. Carries every offending
 * field so the API can return them together in one 400 payload. Mapped to
 * HTTP 400.
 */
public class TaskValidationException extends RuntimeException {

	private final List<FieldValidationError> errors;

	public TaskValidationException(List<FieldValidationError> errors) {
		super("Validation failed");
		this.errors = List.copyOf(errors);
	}

	public List<FieldValidationError> getErrors() {
		return errors;
	}

	/** A single validation failure bound to a request field. */
	public record FieldValidationError(String field, String message) {
	}

}
