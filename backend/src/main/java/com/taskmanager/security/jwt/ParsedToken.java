package com.taskmanager.security.jwt;

import com.taskmanager.user.UserRole;

/** The identity carried by a valid, non-expired JWT. */
public record ParsedToken(String username, UserRole role) {
}
