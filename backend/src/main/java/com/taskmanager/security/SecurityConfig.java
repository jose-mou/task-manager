package com.taskmanager.security;

import com.taskmanager.security.jwt.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Wires the two authentication mechanisms - HTTP Basic for services and JWT
 * bearer for UI users - and the URL-level authorization rules
 * (specs/service-registry-and-task-scoping.md). Resource-level ownership
 * rules that URL matching cannot express (a service acting only on its own
 * record) are enforced in the business layer; see {@link CallerIdentityResolver}.
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
			SecurityErrorHandling errorHandling) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(handling -> handling.authenticationEntryPoint(errorHandling)
				.accessDeniedHandler(errorHandling))
			.httpBasic(basic -> basic.authenticationEntryPoint(errorHandling))
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
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
		return new JwtAuthenticationFilter(jwtService);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
