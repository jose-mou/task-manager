package com.taskmanager.task.web;

/** A single validation failure bound to a request field. */
public record FieldError(String field, String message) {
}
