package com.taskmanager.user;

import com.taskmanager.web.FieldError;

/** Thrown when a password-change request is invalid. Mapped to HTTP 400 naming the offending field. */
public class PasswordValidationException extends RuntimeException {

	private final FieldError error;

	public PasswordValidationException(FieldError error) {
		super(error.field() + ": " + error.message());
		this.error = error;
	}

	public FieldError error() {
		return error;
	}

}
