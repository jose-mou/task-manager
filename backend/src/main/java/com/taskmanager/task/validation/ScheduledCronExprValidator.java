package com.taskmanager.task.validation;

import com.taskmanager.task.web.TaskRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.scheduling.support.CronExpression;

/** Backs {@link ValidScheduledCron}. */
public class ScheduledCronExprValidator implements ConstraintValidator<ValidScheduledCron, TaskRequest> {

	@Override
	public boolean isValid(TaskRequest request, ConstraintValidatorContext context) {
		if (!Boolean.TRUE.equals(request.scheduled()) || isValidCron(request.cronExpr())) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
			.addPropertyNode("cronExpr")
			.addConstraintViolation();
		return false;
	}

	private boolean isValidCron(String cronExpr) {
		return cronExpr != null && !cronExpr.isBlank() && CronExpression.isValidExpression(cronExpr);
	}

}
