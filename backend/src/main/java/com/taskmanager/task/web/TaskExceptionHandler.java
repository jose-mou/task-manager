package com.taskmanager.task.web;

import com.taskmanager.task.DuplicateTaskNameException;
import com.taskmanager.task.TaskNotFoundException;
import com.taskmanager.task.TaskValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Maps task domain exceptions to the HTTP responses frozen in api/openapi.yaml. */
@RestControllerAdvice
public class TaskExceptionHandler {

	@ExceptionHandler(TaskValidationException.class)
	public ResponseEntity<ValidationErrorResponse> handleValidation(TaskValidationException ex) {
		List<FieldError> errors = ex.getErrors().stream().map(e -> new FieldError(e.field(), e.message())).toList();
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

}
