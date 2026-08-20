package com.taskmanager.user.web;

import jakarta.validation.constraints.NotBlank;

/** Payload accepted by {@code POST /api/auth/login}. */
public record LoginRequest(@NotBlank(message = "must not be blank") String username,
		@NotBlank(message = "must not be blank") String password) {
}
