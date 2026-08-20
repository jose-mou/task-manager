package com.taskmanager.web;

/** Generic error payload used for 401, 403, 404 and 409 responses across the API. */
public record ErrorResponse(String message) {
}
