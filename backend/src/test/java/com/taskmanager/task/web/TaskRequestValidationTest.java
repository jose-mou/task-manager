package com.taskmanager.task.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-level proof that {@link TaskRequest}'s declarative Bean Validation
 * constraints enforce every rule from specs/task-management.md, one case per
 * rule. The end-to-end 400 payload shape (message, field names, ordering) is
 * covered by {@link TaskControllerTest} and the acceptance tests.
 */
class TaskRequestValidationTest {

	private static ValidatorFactory validatorFactory;

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidatorFactory() {
		validatorFactory.close();
	}

	private TaskRequest validRequest() {
		return new TaskRequest("Nightly backup", "backup-service", null, null, "/opt/scripts/backup.sh", null, null,
				null);
	}

	private Set<String> fieldsOf(Set<ConstraintViolation<TaskRequest>> violations) {
		return violations.stream().map(violation -> violation.getPropertyPath().toString()).collect(Collectors.toSet());
	}

	@Test
	void validRequestHasNoViolations() {
		assertThat(validator.validate(validRequest())).isEmpty();
	}

	@Test
	void blankNameAndInvalidMaxExecutionsAreBothReported() {
		TaskRequest request = new TaskRequest("   ", "backup-service", null, null, "/opt/scripts/backup.sh", null, 0,
				null);

		assertThat(fieldsOf(validator.validate(request))).contains("name", "maxExecutions");
	}

	@Test
	void blankServiceIsReported() {
		TaskRequest request = new TaskRequest("Nightly backup", " ", null, null, "/opt/scripts/backup.sh", null,
				null, null);

		assertThat(fieldsOf(validator.validate(request))).contains("service");
	}

	@Test
	void blankScriptIsReported() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, null, " ", null, null, null);

		assertThat(fieldsOf(validator.validate(request))).contains("script");
	}

	@Test
	void scheduledTrueWithMissingCronExprIsReported() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, null,
				"/opt/scripts/backup.sh", null, null, true);

		assertThat(fieldsOf(validator.validate(request))).contains("cronExpr");
	}

	@Test
	void scheduledTrueWithSyntacticallyInvalidCronExprIsReported() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, null,
				"/opt/scripts/backup.sh", "not a valid cron expression", null, true);

		assertThat(fieldsOf(validator.validate(request))).contains("cronExpr");
	}

	@Test
	void scheduledTrueWithValidCronExprIsNotReported() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, null,
				"/opt/scripts/backup.sh", "0 0 2 * * *", null, true);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void scheduledFalseDoesNotRequireCronExpr() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, null,
				"/opt/scripts/backup.sh", null, null, false);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void maxExecutionsBelowOneIsReported() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, null,
				"/opt/scripts/backup.sh", null, 0, null);

		assertThat(fieldsOf(validator.validate(request))).contains("maxExecutions");
	}

	@Test
	void maxExecutionsOfOneIsValid() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, null,
				"/opt/scripts/backup.sh", null, 1, null);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void unknownStatusIsReported() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, "PAUSED",
				"/opt/scripts/backup.sh", null, null, null);

		assertThat(fieldsOf(validator.validate(request))).contains("status");
	}

	@Test
	void knownStatusIsValid() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, "COMPLETED",
				"/opt/scripts/backup.sh", null, null, null);

		assertThat(validator.validate(request)).isEmpty();
	}

}
