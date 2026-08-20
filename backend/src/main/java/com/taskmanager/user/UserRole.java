package com.taskmanager.user;

/**
 * Role of a UI user. {@code ADMIN} may write tasks for any service and
 * manage the service registry; {@code USER} grants nothing beyond the
 * anonymous reads (specs/service-registry-and-task-scoping.md, rule 6).
 */
public enum UserRole {

	ADMIN, USER

}
