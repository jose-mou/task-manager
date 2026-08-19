package com.taskmanager.task.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a raw status string, when present, must name one of
 * {@link com.taskmanager.task.TaskStatus}'s values. Applied to a {@code String}
 * (rather than typing the field as {@code TaskStatus} itself) so an unknown
 * value is reported alongside every other offending field in one 400 payload,
 * instead of failing Jackson deserialization alone.
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
		ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTaskStatusValidator.class)
public @interface ValidTaskStatus {

	String message() default "must be one of CREATED, RUNNING, COMPLETED, CANCELED";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
