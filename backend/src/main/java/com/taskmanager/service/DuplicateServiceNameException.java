package com.taskmanager.service;

/**
 * Thrown when another service already uses the given name, compared
 * case-insensitively. Not part of the contract's documented responses for
 * the service endpoints (out of scope of the acceptance criteria), but the
 * database enforces name uniqueness for referential integrity of
 * {@code Task.service}, so a race between two concurrent registrations or
 * renames must still fail cleanly instead of surfacing a raw 500.
 */
public class DuplicateServiceNameException extends RuntimeException {

	public DuplicateServiceNameException(String name) {
		super("A service with name '" + name + "' already exists");
	}

}
