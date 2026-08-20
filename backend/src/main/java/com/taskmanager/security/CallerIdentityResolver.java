package com.taskmanager.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@link CallerIdentity} of the current request from its Spring
 * Security {@link Authentication}. Only ever called from endpoints already
 * restricted to {@code ROLE_ADMIN} or {@code ROLE_SERVICE} at the URL
 * authorization level, so the {@link AccessDeniedException} branch below is
 * defensive: it should never be reachable in practice.
 */
@Component
public class CallerIdentityResolver {

	public CallerIdentity resolve(Authentication authentication) {
		if (authentication != null && hasRole(authentication, SecurityRoles.SERVICE)
				&& authentication.getPrincipal() instanceof ServicePrincipal principal) {
			return new ServiceCaller(principal.id(), principal.name());
		}
		if (authentication != null && hasRole(authentication, SecurityRoles.ADMIN)) {
			return new AdminCaller(authentication.getName());
		}
		throw new AccessDeniedException("Access denied");
	}

	private boolean hasRole(Authentication authentication, String role) {
		String authority = "ROLE_" + role;
		for (GrantedAuthority granted : authentication.getAuthorities()) {
			if (authority.equals(granted.getAuthority())) {
				return true;
			}
		}
		return false;
	}

}
