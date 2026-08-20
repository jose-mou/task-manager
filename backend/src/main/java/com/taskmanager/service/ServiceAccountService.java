package com.taskmanager.service;

import com.taskmanager.security.CallerIdentity;
import com.taskmanager.security.ServiceCaller;
import com.taskmanager.security.crypto.CredentialGenerator;
import com.taskmanager.security.crypto.SecretHasher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Business logic for registering, listing, renaming, deleting and rotating services. */
@Service
public class ServiceAccountService {

	private final ServiceAccountRepository repository;

	private final CredentialGenerator credentialGenerator;

	private final SecretHasher secretHasher;

	private final Clock clock;

	public ServiceAccountService(ServiceAccountRepository repository, CredentialGenerator credentialGenerator,
			SecretHasher secretHasher, Clock clock) {
		this.repository = repository;
		this.credentialGenerator = credentialGenerator;
		this.secretHasher = secretHasher;
		this.clock = clock;
	}

	@Transactional
	public IssuedCredentials register(String name) {
		String apiKey = credentialGenerator.generate();
		String apiSecret = credentialGenerator.generate();
		ServiceAccount account = ServiceAccount.register(name, apiKey, secretHasher.hash(apiSecret), clock.instant());
		account = save(account, name);
		return new IssuedCredentials(account.getId(), account.getName(), apiKey, apiSecret);
	}

	@Transactional(readOnly = true)
	public List<ServiceAccount> listAll() {
		return repository.findAllByOrderByNameAsc();
	}

	@Transactional(readOnly = true)
	public ServiceAccount getById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ServiceNotFoundException(id));
	}

	@Transactional
	public ServiceAccount rename(UUID id, String newName, CallerIdentity caller) {
		ServiceAccount account = getById(id);
		requireOwnAccountWhenServiceCaller(id, caller);
		account.rename(newName, clock.instant());
		return save(account, newName);
	}

	@Transactional
	public void delete(UUID id) {
		if (!repository.existsById(id)) {
			throw new ServiceNotFoundException(id);
		}
		repository.deleteById(id);
	}

	@Transactional
	public IssuedCredentials rotateCredentials(UUID id, CallerIdentity caller) {
		ServiceAccount account = getById(id);
		requireOwnAccountWhenServiceCaller(id, caller);
		String apiKey = credentialGenerator.generate();
		String apiSecret = credentialGenerator.generate();
		account.rotateCredentials(apiKey, secretHasher.hash(apiSecret), clock.instant());
		account = save(account, account.getName());
		return new IssuedCredentials(account.getId(), account.getName(), apiKey, apiSecret);
	}

	/**
	 * Enforces "allowed to an ADMIN JWT or to that same service's own
	 * credentials; another service's credentials get 403"
	 * (specs/service-registry-and-task-scoping.md, rules 2 and API impact for
	 * PUT/rotate). An ADMIN caller is always allowed.
	 */
	private void requireOwnAccountWhenServiceCaller(UUID targetId, CallerIdentity caller) {
		if (caller instanceof ServiceCaller serviceCaller && !serviceCaller.serviceId().equals(targetId)) {
			throw new AccessDeniedException("Access denied");
		}
	}

	private ServiceAccount save(ServiceAccount account, String name) {
		try {
			return repository.saveAndFlush(account);
		}
		catch (DataIntegrityViolationException ex) {
			if (isUniqueViolation(ex)) {
				throw new DuplicateServiceNameException(name);
			}
			throw ex;
		}
	}

	private boolean isUniqueViolation(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
				return true;
			}
			if (cause.getCause() == cause) {
				return false;
			}
		}
		return false;
	}

}
