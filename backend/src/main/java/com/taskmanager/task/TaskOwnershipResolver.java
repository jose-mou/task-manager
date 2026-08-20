package com.taskmanager.task;

import com.taskmanager.security.AdminCaller;
import com.taskmanager.security.CallerIdentity;
import com.taskmanager.security.ServiceCaller;
import com.taskmanager.service.ServiceAccount;
import com.taskmanager.service.ServiceAccountRepository;
import com.taskmanager.web.FieldError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the owning service of a task write from the caller's identity and
 * the request payload (specs/service-registry-and-task-scoping.md, rules
 * 9-10):
 *
 * <ul>
 * <li>Service credentials: the owner is the authenticated service; the
 * payload's {@code service} field is ignored, whatever it contains.</li>
 * <li>ADMIN JWT: the payload's {@code service} field is required and must
 * name a registered service; otherwise 400 naming {@code service}.</li>
 * </ul>
 */
@Component
public class TaskOwnershipResolver {

	private final ServiceAccountRepository serviceAccountRepository;

	public TaskOwnershipResolver(ServiceAccountRepository serviceAccountRepository) {
		this.serviceAccountRepository = serviceAccountRepository;
	}

	public UUID resolveOwningServiceId(String requestedServiceName, CallerIdentity caller) {
		if (caller instanceof ServiceCaller serviceCaller) {
			return serviceCaller.serviceId();
		}
		if (caller instanceof AdminCaller) {
			return resolveForAdmin(requestedServiceName);
		}
		// Defensive: task write endpoints are already restricted to ADMIN/SERVICE
		// roles at the URL authorization level, so no other CallerIdentity reaches here.
		throw new AccessDeniedException("Access denied");
	}

	private UUID resolveForAdmin(String requestedServiceName) {
		if (requestedServiceName == null || requestedServiceName.isBlank()) {
			throw new TaskRequestValidationException(new FieldError("service", "must not be blank"));
		}
		return serviceAccountRepository.findByName(requestedServiceName)
			.map(ServiceAccount::getId)
			.orElseThrow(
					() -> new TaskRequestValidationException(new FieldError("service", "must name a registered service")));
	}

}
