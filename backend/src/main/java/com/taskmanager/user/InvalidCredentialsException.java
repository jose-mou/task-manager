package com.taskmanager.user;

/**
 * Thrown when a login attempt's username or password is wrong. Mapped to
 * HTTP 401; the message never distinguishes which of the two was wrong
 * (specs/service-registry-and-task-scoping.md, rule 3).
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid username or password");
	}

}
