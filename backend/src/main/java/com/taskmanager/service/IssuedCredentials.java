package com.taskmanager.service;

import java.util.UUID;

/** The one-time {@code apiKey}/{@code apiSecret} pair returned by registration and rotation. */
public record IssuedCredentials(UUID id, String name, String apiKey, String apiSecret) {
}
