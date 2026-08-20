package com.taskmanager.security;

/**
 * The identity of an authenticated caller performing a write, resolved from
 * the Spring Security {@code Authentication} of the current request.
 *
 * There are exactly two kinds (specs/service-registry-and-task-scoping.md):
 * an ADMIN UI user (JWT) or a service (HTTP Basic credentials). A USER-role
 * JWT never reaches this point: it is rejected with 403 at the URL
 * authorization level before any controller code runs.
 */
public sealed interface CallerIdentity permits AdminCaller, ServiceCaller {
}
