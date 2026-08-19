package com.taskmanager.task;

import java.util.UUID;

/** Thrown when no task exists with the given id. Mapped to HTTP 404. */
public class TaskNotFoundException extends RuntimeException {

	public TaskNotFoundException(UUID id) {
		super("Task " + id + " not found");
	}

}
