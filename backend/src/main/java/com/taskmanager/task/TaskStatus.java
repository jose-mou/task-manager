package com.taskmanager.task;

/**
 * Lifecycle status of a task. Any value is accepted on create and update
 * regardless of the current one; no transition is validated (out of scope
 * for this feature, see specs/task-management.md).
 */
public enum TaskStatus {

	CREATED, RUNNING, COMPLETED, CANCELED;

	/**
	 * Case-sensitive lookup used by validation to tell an unknown status apart
	 * from a Jackson/enum mapping failure, so every offending field of a
	 * request can be reported together in a single 400 payload.
	 */
	public static boolean isValid(String value) {
		if (value == null) {
			return false;
		}
		for (TaskStatus status : values()) {
			if (status.name().equals(value)) {
				return true;
			}
		}
		return false;
	}

}
