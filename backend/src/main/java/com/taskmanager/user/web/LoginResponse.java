package com.taskmanager.user.web;

import com.taskmanager.security.jwt.IssuedToken;
import com.taskmanager.user.UserRole;

import java.time.Instant;

/** The issued JWT and the data the UI needs to use it. */
public record LoginResponse(String token, Instant expiresAt, UserRole role) {

	public static LoginResponse from(IssuedToken issued) {
		return new LoginResponse(issued.token(), issued.expiresAt(), issued.role());
	}

}
