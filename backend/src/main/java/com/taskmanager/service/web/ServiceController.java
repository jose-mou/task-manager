package com.taskmanager.service.web;

import com.taskmanager.security.CallerIdentity;
import com.taskmanager.security.CallerIdentityResolver;
import com.taskmanager.service.IssuedCredentials;
import com.taskmanager.service.ServiceAccount;
import com.taskmanager.service.ServiceAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST layer for the service registry. Authorization is enforced by
 * {@link com.taskmanager.security.SecurityConfig} at the URL level (which
 * roles may call each operation at all); the "own service only" ownership
 * rule for PUT/rotate is enforced by {@link ServiceAccountService}.
 */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

	private final ServiceAccountService serviceAccountService;

	private final CallerIdentityResolver callerIdentityResolver;

	public ServiceController(ServiceAccountService serviceAccountService, CallerIdentityResolver callerIdentityResolver) {
		this.serviceAccountService = serviceAccountService;
		this.callerIdentityResolver = callerIdentityResolver;
	}

	@GetMapping
	public List<ServiceResponse> list() {
		return serviceAccountService.listAll().stream().map(ServiceResponse::from).toList();
	}

	@PostMapping
	public ResponseEntity<ServiceCredentialsResponse> register(@Valid @RequestBody ServiceRequest request) {
		IssuedCredentials credentials = serviceAccountService.register(request.name());
		return ResponseEntity.status(HttpStatus.CREATED).body(ServiceCredentialsResponse.from(credentials));
	}

	@PutMapping("/{id}")
	public ServiceResponse rename(@PathVariable UUID id, @Valid @RequestBody ServiceRequest request,
			Authentication authentication) {
		CallerIdentity caller = callerIdentityResolver.resolve(authentication);
		ServiceAccount renamed = serviceAccountService.rename(id, request.name(), caller);
		return ServiceResponse.from(renamed);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		serviceAccountService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/credentials")
	public ServiceCredentialsResponse rotateCredentials(@PathVariable UUID id, Authentication authentication) {
		CallerIdentity caller = callerIdentityResolver.resolve(authentication);
		IssuedCredentials credentials = serviceAccountService.rotateCredentials(id, caller);
		return ServiceCredentialsResponse.from(credentials);
	}

}
