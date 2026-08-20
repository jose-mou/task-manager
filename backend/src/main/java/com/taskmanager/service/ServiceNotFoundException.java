package com.taskmanager.service;

import java.util.UUID;

/** Thrown when no service exists with the given id. Mapped to HTTP 404. */
public class ServiceNotFoundException extends RuntimeException {

	public ServiceNotFoundException(UUID id) {
		super("Service " + id + " not found");
	}

}
