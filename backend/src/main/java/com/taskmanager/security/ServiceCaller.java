package com.taskmanager.security;

import java.util.UUID;

/** A machine identity, authenticated with its own {@code apiKey}/{@code apiSecret}. */
public record ServiceCaller(UUID serviceId, String serviceName) implements CallerIdentity {
}
