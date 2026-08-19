package com.taskmanager.task;

/**
 * Thrown when another task already uses the given name, compared
 * case-insensitively. Mapped to HTTP 409.
 */
public class DuplicateTaskNameException extends RuntimeException {

	public DuplicateTaskNameException(String name) {
		super("A task with name '" + name + "' already exists");
	}

}
