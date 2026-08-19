package com.taskmanager.task.validation;

import com.taskmanager.task.TaskStatus;
import com.taskmanager.task.TaskValidationException.FieldValidationError;
import com.taskmanager.task.web.TaskRequest;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates a {@link TaskRequest} against every rule from
 * specs/task-management.md, collecting all offending fields instead of
 * failing fast, so the API can return them together in a single 400
 * payload.
 */
@Component
public class TaskRequestValidator {

	public List<FieldValidationError> validate(TaskRequest request) {
		List<FieldValidationError> errors = new ArrayList<>();

		if (isBlank(request.name())) {
			errors.add(new FieldValidationError("name", "must not be blank"));
		}
		if (isBlank(request.service())) {
			errors.add(new FieldValidationError("service", "must not be blank"));
		}
		if (isBlank(request.script())) {
			errors.add(new FieldValidationError("script", "must not be blank"));
		}
		if (request.status() != null && !TaskStatus.isValid(request.status())) {
			errors.add(new FieldValidationError("status", "must be one of CREATED, RUNNING, COMPLETED, CANCELED"));
		}
		if (Boolean.TRUE.equals(request.scheduled()) && !isValidCron(request.cronExpr())) {
			errors.add(new FieldValidationError("cronExpr",
					"must be a valid 6-field cron expression when the task is scheduled"));
		}
		if (request.maxExecutions() != null && request.maxExecutions() < 1) {
			errors.add(new FieldValidationError("maxExecutions", "must be greater than or equal to 1"));
		}

		return errors;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private boolean isValidCron(String cronExpr) {
		return !isBlank(cronExpr) && CronExpression.isValidExpression(cronExpr);
	}

}
