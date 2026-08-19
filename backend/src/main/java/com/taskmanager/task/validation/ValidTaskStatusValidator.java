package com.taskmanager.task.validation;

import com.taskmanager.task.TaskStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Backs {@link ValidTaskStatus}; a missing status is left to whichever rule requires it. */
public class ValidTaskStatusValidator implements ConstraintValidator<ValidTaskStatus, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || TaskStatus.isValid(value);
	}

}
