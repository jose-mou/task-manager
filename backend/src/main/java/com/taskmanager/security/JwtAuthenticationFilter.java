package com.taskmanager.security;

import com.taskmanager.security.jwt.JwtService;
import com.taskmanager.security.jwt.ParsedToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates {@code Authorization: Bearer <token>} requests
 * (specs/service-registry-and-task-scoping.md, rule 3). A missing, malformed
 * or expired token is left unauthenticated rather than rejected here: the
 * request simply falls through as anonymous, and the URL authorization rules
 * (or an ownership check further down) turn that into the contract's 401/403
 * as appropriate - this filter never itself writes an error response.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		bearerToken(request).flatMap(jwtService::parse).ifPresent(this::authenticate);
		filterChain.doFilter(request, response);
	}

	private Optional<String> bearerToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			return Optional.of(header.substring(BEARER_PREFIX.length()));
		}
		return Optional.empty();
	}

	private void authenticate(ParsedToken parsed) {
		var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + parsed.role().name()));
		var authentication = new UsernamePasswordAuthenticationToken(parsed.username(), null, authorities);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

}
