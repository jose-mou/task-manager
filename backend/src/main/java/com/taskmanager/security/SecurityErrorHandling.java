package com.taskmanager.security;

import com.taskmanager.web.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Writes the contract's {@code ErrorResponse} JSON body for 401/403 failures.
 *
 * Deliberately does not send a {@code WWW-Authenticate} challenge on 401:
 * api/openapi.yaml's Unauthorized response is explicit that none is
 * returned, so browsers never pop their own native credential dialog and the
 * UI can redirect to its own login screen instead.
 */
@Component
public class SecurityErrorHandling implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public SecurityErrorHandling(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		writeError(response, HttpStatus.UNAUTHORIZED, "Authentication required");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
			throws IOException, ServletException {
		writeError(response, HttpStatus.FORBIDDEN, "Access denied");
	}

	private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message));
	}

}
