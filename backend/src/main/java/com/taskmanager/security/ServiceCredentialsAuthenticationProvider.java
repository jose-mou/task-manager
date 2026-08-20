package com.taskmanager.security;

import com.taskmanager.security.crypto.SecretHasher;
import com.taskmanager.service.ServiceAccount;
import com.taskmanager.service.ServiceAccountRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Authenticates HTTP Basic credentials as a service: {@code apiKey} as
 * username, {@code apiSecret} as password (specs/service-registry-and-task-scoping.md,
 * rule 3). Unknown apiKey, wrong or rotated-away apiSecret are all
 * indistinguishable {@link BadCredentialsException} (401), never revealing
 * which part was wrong.
 */
@Component
public class ServiceCredentialsAuthenticationProvider implements AuthenticationProvider {

	private final ServiceAccountRepository repository;

	private final SecretHasher secretHasher;

	public ServiceCredentialsAuthenticationProvider(ServiceAccountRepository repository, SecretHasher secretHasher) {
		this.repository = repository;
		this.secretHasher = secretHasher;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String apiKey = authentication.getName();
		String apiSecret = String.valueOf(authentication.getCredentials());

		Optional<ServiceAccount> account = repository.findByApiKey(apiKey);
		if (account.isEmpty() || !secretHasher.matches(apiSecret, account.get().getApiSecretHash())) {
			throw new BadCredentialsException("Invalid credentials");
		}

		ServicePrincipal principal = new ServicePrincipal(account.get().getId(), account.get().getName());
		return new UsernamePasswordAuthenticationToken(principal, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + SecurityRoles.SERVICE)));
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}

}
