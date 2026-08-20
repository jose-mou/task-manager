package com.taskmanager.task;

import java.util.UUID;

/**
 * Resolved, already-defaulted values for creating or updating a task:
 * unlike {@link com.taskmanager.task.web.TaskRequest}, {@code status} is a
 * real {@link TaskStatus} and {@code scheduled} a primitive boolean, both
 * with their defaults already applied, and {@code serviceId} is the already
 * resolved id of the owning {@link com.taskmanager.service.ServiceAccount}
 * (see {@link TaskOwnershipResolver}), not the raw request string.
 */
public record TaskFields(String name, UUID serviceId, String description, TaskStatus status, String script,
		String cronExpr, Integer maxExecutions, boolean scheduled) {
}
