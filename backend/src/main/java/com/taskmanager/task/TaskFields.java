package com.taskmanager.task;

/**
 * Resolved, already-defaulted values for creating or updating a task:
 * unlike {@link com.taskmanager.task.web.TaskRequest}, {@code status} is a
 * real {@link TaskStatus} and {@code scheduled} a primitive boolean, both
 * with their defaults already applied.
 */
public record TaskFields(String name, String service, String description, TaskStatus status, String script,
		String cronExpr, Integer maxExecutions, boolean scheduled) {
}
