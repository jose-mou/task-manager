package com.taskmanager.task.web;

/** Generic error payload used for 404 and 409 responses. */
public record ErrorResponse(String message) {
}
