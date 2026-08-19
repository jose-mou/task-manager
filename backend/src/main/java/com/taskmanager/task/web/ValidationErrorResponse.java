package com.taskmanager.task.web;

import java.util.List;

/** Returned with 400 when the payload is invalid; {@code errors} lists every offending field. */
public record ValidationErrorResponse(String message, List<FieldError> errors) {
}
