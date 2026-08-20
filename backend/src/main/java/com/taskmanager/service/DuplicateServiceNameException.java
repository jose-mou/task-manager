package com.taskmanager.service;

import com.taskmanager.web.FieldError;

/**
 * Thrown when another service already uses the given name, compared
 * case-insensitively by the {@code ux_services_name_ci} index.
 *
 * A service name is what {@code Task.service} carries and what an ADMIN names
 * when creating a task, so it must resolve to exactly one service. The name is
 * therefore rejected as an invalid payload field: api/openapi.yaml declares no
 * 409 on the service endpoints, only the {@code ServiceValidationError} 400
 * that lists every offending field.
 */
public class DuplicateServiceNameException extends RuntimeException {

	private final FieldError error;

	public DuplicateServiceNameException(String name) {
		super("A service with name '" + name + "' already exists");
		this.error = new FieldError("name", "must not already be used by another service");
	}

	public FieldError error() {
		return error;
	}

}
