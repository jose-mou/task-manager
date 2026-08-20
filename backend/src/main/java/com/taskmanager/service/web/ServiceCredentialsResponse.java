package com.taskmanager.service.web;

import com.taskmanager.service.IssuedCredentials;

import java.util.UUID;

/** One-time credential payload, returned only by registration and rotation. */
public record ServiceCredentialsResponse(UUID id, String name, String apiKey, String apiSecret) {

	public static ServiceCredentialsResponse from(IssuedCredentials credentials) {
		return new ServiceCredentialsResponse(credentials.id(), credentials.name(), credentials.apiKey(),
				credentials.apiSecret());
	}

}
