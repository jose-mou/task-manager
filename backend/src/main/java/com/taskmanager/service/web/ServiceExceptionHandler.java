package com.taskmanager.service.web;

import com.taskmanager.service.DuplicateServiceNameException;
import com.taskmanager.service.ServiceNotFoundException;
import com.taskmanager.web.ErrorResponse;
import com.taskmanager.web.FieldError;
import com.taskmanager.web.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/** Maps service-registry domain exceptions to the HTTP responses frozen in api/openapi.yaml. */
@RestControllerAdvice(assignableTypes = ServiceController.class)
public class ServiceExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
		List<FieldError> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fieldError -> new FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
			.toList();
		return ResponseEntity.badRequest().body(ValidationErrorResponse.of(errors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ValidationErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest()
			.body(ValidationErrorResponse.of(new FieldError("body", "must be a valid JSON object")));
	}

	@ExceptionHandler(ServiceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ServiceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
	}

	/**
	 * A name already taken by another service is reported as the contract's
	 * {@code ServiceValidationError} (400 naming {@code name}); api/openapi.yaml
	 * declares no 409 for {@code POST /api/services} nor {@code PUT /api/services/{id}}.
	 */
	@ExceptionHandler(DuplicateServiceNameException.class)
	public ResponseEntity<ValidationErrorResponse> handleDuplicate(DuplicateServiceNameException ex) {
		return ResponseEntity.badRequest().body(ValidationErrorResponse.of(ex.error()));
	}

	/** A path {@code id} that is not a UUID cannot match any stored service: reported as 404, like an unknown id. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMalformedId(MethodArgumentTypeMismatchException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(new ErrorResponse("Service " + ex.getValue() + " not found"));
	}

}
