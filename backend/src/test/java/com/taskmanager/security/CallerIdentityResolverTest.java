package com.taskmanager.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallerIdentityResolverTest {

	private final CallerIdentityResolver resolver = new CallerIdentityResolver();

	@Test
	void resolvesAnAdminJwtToAnAdminCaller() {
		var authentication = new UsernamePasswordAuthenticationToken("admin", null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

		CallerIdentity caller = resolver.resolve(authentication);

		assertThat(caller).isEqualTo(new AdminCaller("admin"));
	}

	@Test
	void resolvesServiceCredentialsToAServiceCaller() {
		UUID id = UUID.randomUUID();
		ServicePrincipal principal = new ServicePrincipal(id, "backup-service");
		var authentication = new UsernamePasswordAuthenticationToken(principal, null,
				List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));

		CallerIdentity caller = resolver.resolve(authentication);

		assertThat(caller).isEqualTo(new ServiceCaller(id, "backup-service"));
	}

	@Test
	void rejectsAnAuthenticationWithNeitherRole() {
		var authentication = new TestingAuthenticationToken("plain-user", null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));

		assertThatThrownBy(() -> resolver.resolve(authentication)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void rejectsANullAuthentication() {
		assertThatThrownBy(() -> resolver.resolve(null)).isInstanceOf(AccessDeniedException.class);
	}

}
