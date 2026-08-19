package com.taskmanager.task.web;

import com.taskmanager.task.validation.ValidScheduledCron;
import com.taskmanager.task.validation.ValidTaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload accepted by {@code POST /api/tasks} and {@code PUT /api/tasks/{id}}.
 * {@code status} is kept as a raw string (rather than typed as
 * {@link com.taskmanager.task.TaskStatus}) so an unknown status value is a
 * validation error reported alongside every other offending field, instead
 * of a hard Jackson deserialization failure reported alone; {@link ValidTaskStatus}
 * enforces the same rule declaratively. {@code scheduled} stays a {@link Boolean}
 * for the same reason: {@link ValidScheduledCron} reads it to decide whether
 * {@code cronExpr} is required.
 */
@ValidScheduledCron
public record TaskRequest(@NotBlank(message = "must not be blank") String name,
		@NotBlank(message = "must not be blank") String service, String description,
		@ValidTaskStatus String status, @NotBlank(message = "must not be blank") String script, String cronExpr,
		@Min(value = 1, message = "must be greater than or equal to 1") Integer maxExecutions, Boolean scheduled) {
}
