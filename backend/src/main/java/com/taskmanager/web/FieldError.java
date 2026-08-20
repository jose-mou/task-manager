package com.taskmanager.web;

/** A single validation failure bound to a request field. */
public record FieldError(String field, String message) {
}
