package com.taskmanager.security;

import java.util.UUID;

/**
 * {@code Authentication#getPrincipal()} for a request authenticated with
 * service credentials (HTTP Basic), set by {@link ServiceCredentialsAuthenticationProvider}.
 */
public record ServicePrincipal(UUID id, String name) {
}
