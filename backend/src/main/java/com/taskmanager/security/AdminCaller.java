package com.taskmanager.security;

/** An ADMIN UI user, authenticated with a JWT. */
public record AdminCaller(String username) implements CallerIdentity {
}
