package com.taskmanager.user.web;

import com.taskmanager.user.InvalidCredentialsException;
import com.taskmanager.user.PasswordValidationException;
import com.taskmanager.web.ErrorResponse;
import com.taskmanager.web.FieldError;
import com.taskmanager.web.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Maps authentication/user domain exceptions to the HTTP responses frozen in api/openapi.yaml. */
@RestControllerAdvice(assignableTypes = { AuthController.class, UserController.class })
public class UserExceptionHandler {

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

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(PasswordValidationException.class)
	public ResponseEntity<ValidationErrorResponse> handlePasswordValidation(PasswordValidationException ex) {
		return ResponseEntity.badRequest().body(ValidationErrorResponse.of(ex.error()));
	}

}
