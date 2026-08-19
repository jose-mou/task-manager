package com.taskmanager.task.validation;

import com.taskmanager.task.TaskValidationException.FieldValidationError;
import com.taskmanager.task.web.TaskRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRequestValidatorTest {

	private final TaskRequestValidator validator = new TaskRequestValidator();

	private TaskRequest validRequest() {
		return new TaskRequest("Nightly backup", "backup-service", null, null, "/opt/scripts/backup.sh", null, null,
				null);
	}

	@Test
	void validRequestHasNoErrors() {
		assertThat(validator.validate(validRequest())).isEmpty();
	}

	@Test
	void blankNameAndInvalidMaxExecutionsAreBothReported() {
		TaskRequest request = new TaskRequest("   ", "backup-service", null, null, "/opt/scripts/backup.sh", null, 0,
				null);

		List<FieldValidationError> errors = validator.validate(request);

		assertThat(errors).extracting(FieldValidationError::field).contains("name", "maxExecutions");
	}

	@Test
	void blankServiceIsReported() {
		TaskRequest request = new TaskRequest("Nightly backup", " ", null, null, "/opt/scripts/backup.sh", null,
				null, null);

		assertThat(validator.validate(request)).extracting(FieldValidationError::field).contains("service");
	}

	@Test
	void blankScriptIsReported() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, null, " ", null, null, null);

		assertThat(validator.validate(request)).extracting(FieldValidationError::field).contains("script");
	}

	@Test
	void scheduledTrueWithMissingCronExprIsReported() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, null,
				"/opt/scripts/backup.sh", null, null, true);

		assertThat(validator.validate(request)).extracting(FieldValidationError::field).contains("cronExpr");
	}

	@Test
	void scheduledTrueWithSyntacticallyInvalidCronExprIsReported() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, null,
				"/opt/scripts/backup.sh", "not a valid cron expression", null, true);

		assertThat(validator.validate(request)).extracting(FieldValidationError::field).contains("cronExpr");
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

		assertThat(validator.validate(request)).extracting(FieldValidationError::field).contains("maxExecutions");
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

		assertThat(validator.validate(request)).extracting(FieldValidationError::field).contains("status");
	}

	@Test
	void knownStatusIsValid() {
		TaskRequest request = new TaskRequest("Nightly backup", "backup-service", null, "COMPLETED",
				"/opt/scripts/backup.sh", null, null, null);

		assertThat(validator.validate(request)).isEmpty();
	}

}
