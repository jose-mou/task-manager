package com.taskmanager.web;

import java.util.List;

/** Returned with 400 when the payload is invalid; {@code errors} lists every offending field. */
public record ValidationErrorResponse(String message, List<FieldError> errors) {

	public static ValidationErrorResponse of(List<FieldError> errors) {
		return new ValidationErrorResponse("Validation failed", errors);
	}

	public static ValidationErrorResponse of(FieldError error) {
		return of(List.of(error));
	}

}
