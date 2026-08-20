package com.taskmanager.task.web;

import com.taskmanager.task.Task;
import com.taskmanager.task.TaskStatus;

import java.time.Instant;
import java.util.UUID;

/** A stored task, as returned by the API. Mirrors the {@code Task} schema of api/openapi.yaml. */
public record TaskResponse(UUID id, String name, Instant creationDate, Instant modificationDate, String service,
		String description, TaskStatus status, String script, String cronExpr, Integer maxExecutions,
		boolean scheduled) {

	public static TaskResponse from(Task task) {
		return new TaskResponse(task.getId(), task.getName(), task.getCreationDate(), task.getModificationDate(),
				task.getOwningService().getName(), task.getDescription(), task.getStatus(), task.getScript(),
				task.getCronExpr(), task.getMaxExecutions(), task.isScheduled());
	}

}
