package com.taskmanager.security;

import com.taskmanager.security.jwt.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Wires the two authentication mechanisms - HTTP Basic for services and JWT
 * bearer for UI users - and the URL-level authorization rules
 * (specs/service-registry-and-task-scoping.md). Resource-level ownership
 * rules that URL matching cannot express (a service acting only on its own
 * record) are enforced in the business layer; see {@link CallerIdentityResolver}.
 *
 * Both authentication filters are deliberately non-failing: credentials that
 * do not authenticate leave the request anonymous instead of ending it with an
 * error. That is what makes "credentials are ignored when present" true of the
 * anonymous reads (rule 7, and the {@code security: []} of GET /api/tasks in
 * api/openapi.yaml) - a caller holding a rotated-away apiSecret keeps reading
 * normally. Writes are unaffected: an anonymous request to a protected URL
 * still ends at {@link SecurityErrorHandling} as 401.
 *
 * The filters are built here rather than exposed as beans on purpose: a
 * {@code Filter} bean would additionally be auto-registered by Spring Boot in
 * the plain servlet chain, outside this security chain.
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
			ServiceCredentialsAuthenticationProvider serviceCredentialsAuthenticationProvider,
			SecurityErrorHandling errorHandling) throws Exception {
		AuthenticationManager serviceAuthenticationManager = new ProviderManager(
				serviceCredentialsAuthenticationProvider);
		http.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(handling -> handling.authenticationEntryPoint(errorHandling)
				.accessDeniedHandler(errorHandling))
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authorize -> authorize
				// Spring Boot's default error forward, so failures still render as JSON.
				.requestMatchers("/error")
				.permitAll()
				// Anonymous and unrestricted (rules 3, 7).
				.requestMatchers(HttpMethod.POST, "/api/auth/login")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/tasks", "/api/tasks/**")
				.permitAll()
				// Task writes: ADMIN or a service's own credentials; USER is 403 (rule 8).
				.requestMatchers(HttpMethod.POST, "/api/tasks")
				.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.SERVICE)
				.requestMatchers(HttpMethod.PUT, "/api/tasks/**")
				.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.SERVICE)
				// Own password change: any authenticated UI user, never service credentials.
				.requestMatchers(HttpMethod.POST, "/api/users/me/password")
				.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.USER)
				// Service registry: registration/listing/deletion are ADMIN-only; rename and
				// credential rotation are also allowed to that same service's own credentials
				// (ownership itself is checked in ServiceAccountService).
				.requestMatchers(HttpMethod.GET, "/api/services")
				.hasRole(SecurityRoles.ADMIN)
				.requestMatchers(HttpMethod.POST, "/api/services")
				.hasRole(SecurityRoles.ADMIN)
				.requestMatchers(HttpMethod.DELETE, "/api/services/*")
				.hasRole(SecurityRoles.ADMIN)
				.requestMatchers(HttpMethod.PUT, "/api/services/*")
				.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.SERVICE)
				.requestMatchers(HttpMethod.POST, "/api/services/*/credentials")
				.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.SERVICE)
				.anyRequest()
				.denyAll())
			.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
			// The single-argument constructor is the non-failing one: on bad
			// credentials it clears the context and continues the chain instead
			// of calling an entry point (see the class javadoc above).
			.addFilterBefore(new BasicAuthenticationFilter(serviceAuthenticationManager),
					UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
