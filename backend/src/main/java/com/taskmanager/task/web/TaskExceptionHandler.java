package com.taskmanager.task.web;

import com.taskmanager.task.DuplicateTaskNameException;
import com.taskmanager.task.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Maps task domain exceptions to the HTTP responses frozen in api/openapi.yaml. */
@RestControllerAdvice(assignableTypes = TaskController.class)
public class TaskExceptionHandler {

	/**
	 * Declaration order of {@link TaskRequest}'s fields, used only to make the
	 * {@code errors} list deterministic: Bean Validation does not guarantee the
	 * order in which it reports violations from several constraints.
	 */
	private static final List<String> FIELD_ORDER = List.of("name", "service", "description", "status", "script",
			"cronExpr", "maxExecutions", "scheduled");

	/**
	 * Maps every field-level Bean Validation failure (declarative constraints on
	 * {@link TaskRequest}, including the class-level {@link
	 * com.taskmanager.task.validation.ValidScheduledCron} rule reported on the
	 * {@code cronExpr} property) to the same 400 payload shape the contract has
	 * always returned.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
		List<FieldError> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fieldError -> new FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
			.sorted(Comparator.comparingInt(error -> FIELD_ORDER.indexOf(error.field())))
			.toList();
		return ResponseEntity.badRequest().body(new ValidationErrorResponse("Validation failed", errors));
	}

	@ExceptionHandler(TaskNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(TaskNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(DuplicateTaskNameException.class)
	public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateTaskNameException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
	}

	/**
	 * A body that Jackson cannot read (missing, not JSON, or a field whose JSON
	 * type does not match the contract, such as a non-numeric
	 * {@code maxExecutions}) is a client-side payload error: the contract answers
	 * it with 400 and the same {@code ValidationErrorResponse} shape as any other
	 * validation failure, never with a container error page.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ValidationErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
		FieldError error = offendingField(ex).map(field -> new FieldError(field, "must be of the expected type"))
			.orElseGet(() -> new FieldError("body", "must be a valid JSON object"));
		return ResponseEntity.badRequest().body(new ValidationErrorResponse("Validation failed", List.of(error)));
	}

	/**
	 * An {@code id} that is not a UUID cannot match any stored task, so it is
	 * reported as 404 like any other unknown id; the contract declares no 400 for
	 * the path parameter.
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMalformedId(MethodArgumentTypeMismatchException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(new ErrorResponse("Task " + ex.getValue() + " not found"));
	}

	private Optional<String> offendingField(HttpMessageNotReadableException ex) {
		for (Throwable cause = ex.getCause(); cause != null && cause != cause.getCause(); cause = cause.getCause()) {
			if (cause instanceof JacksonException jacksonException) {
				return jacksonException.getPath()
					.stream()
					.map(JacksonException.Reference::getPropertyName)
					.filter(Objects::nonNull)
					.reduce((parent, child) -> parent + "." + child);
			}
		}
		return Optional.empty();
	}

}
