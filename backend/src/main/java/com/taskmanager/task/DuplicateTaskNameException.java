package com.taskmanager.task;

/**
 * Thrown when another task of the same owning service already uses the given
 * name, compared case-insensitively (specs/service-registry-and-task-scoping.md,
 * rule 10). Mapped to HTTP 409.
 */
public class DuplicateTaskNameException extends RuntimeException {

	public DuplicateTaskNameException(String name, String serviceName) {
		super("A task with name '" + name + "' already exists for service '" + serviceName + "'");
	}

}
