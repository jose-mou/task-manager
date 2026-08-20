package com.taskmanager.service.web;

import jakarta.validation.constraints.NotBlank;

/** Payload accepted by {@code POST /api/services} and {@code PUT /api/services/{id}}. */
public record ServiceRequest(@NotBlank(message = "must not be blank") String name) {
}
