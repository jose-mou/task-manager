package com.taskmanager.service.web;

import com.taskmanager.service.ServiceAccount;

import java.time.Instant;
import java.util.UUID;

/** A registered service, as returned by the API. Never carries {@code apiKey} or {@code apiSecret}. */
public record ServiceResponse(UUID id, String name, Instant creationDate, Instant modificationDate) {

	public static ServiceResponse from(ServiceAccount account) {
		return new ServiceResponse(account.getId(), account.getName(), account.getCreationDate(),
				account.getModificationDate());
	}

}
