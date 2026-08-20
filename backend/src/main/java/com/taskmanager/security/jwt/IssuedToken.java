package com.taskmanager.security.jwt;

import com.taskmanager.user.UserRole;

import java.time.Instant;

/** A freshly-issued JWT together with the data the caller needs to use it. */
public record IssuedToken(String token, Instant expiresAt, UserRole role) {
}
