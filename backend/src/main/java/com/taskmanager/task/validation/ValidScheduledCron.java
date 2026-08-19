package com.taskmanager.task.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level cross-field rule: when {@code scheduled} is {@code true}, {@code cronExpr}
 * must be present and a valid Spring 6-field cron expression. The violation is reported
 * on the {@code cronExpr} property (see {@link ScheduledCronExprValidator}) so the 400
 * payload names that field, not the whole request object.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ScheduledCronExprValidator.class)
public @interface ValidScheduledCron {

	String message() default "must be a valid 6-field cron expression when the task is scheduled";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
