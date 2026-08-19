package com.taskmanager.task.web;

/**
 * Payload accepted by {@code POST /api/tasks} and {@code PUT /api/tasks/{id}}.
 * Fields are kept as raw strings/wrappers (rather than typed as
 * {@link com.taskmanager.task.TaskStatus}) so an unknown status value is a
 * validation error reported alongside every other offending field, instead
 * of a hard Jackson deserialization failure reported alone.
 */
public record TaskRequest(String name, String service, String description, String status, String script,
		String cronExpr, Integer maxExecutions, Boolean scheduled) {
}
