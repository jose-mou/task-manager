package com.taskmanager.acceptance.support;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared HTTP helpers for the service-registry/authentication acceptance tests.
 *
 * Every helper talks to the API exactly as {@code api/openapi.yaml} defines it (paths,
 * payloads, status codes) - no production type is ever referenced, only plain HTTP over
 * {@link TestRestTemplate}. Keeping this in one place avoids duplicating the same request
 * plumbing across every acceptance test class that needs an admin JWT or a registered
 * service's credentials as a precondition.
 */
public final class AcceptanceTestSupport {

	public static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
			new ParameterizedTypeReference<>() {
			};

	public static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_TYPE =
			new ParameterizedTypeReference<>() {
			};

	public static final String DEFAULT_ADMIN_USERNAME = "admin";
	public static final String DEFAULT_ADMIN_PASSWORD = "admin";

	private AcceptanceTestSupport() {
	}

	public static String uniqueName(String prefix) {
		return prefix + "-" + UUID.randomUUID();
	}

	// ---- authentication -------------------------------------------------

	public static ResponseEntity<Map<String, Object>> login(TestRestTemplate restTemplate, String username,
			String password) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("username", username);
		payload.put("password", password);
		return jsonExchange(restTemplate, "/api/auth/login", HttpMethod.POST, new HttpHeaders(), payload);
	}

	/** Logs in with the given credentials and returns the issued JWT, failing loudly if login did not succeed. */
	public static String loginToken(TestRestTemplate restTemplate, String username, String password) {
		ResponseEntity<Map<String, Object>> response = login(restTemplate, username, password);
		if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
			throw new IllegalStateException(
					"Expected 200 logging in as '" + username + "' but got " + response.getStatusCode()
							+ " with body " + response.getBody());
		}
		return (String) response.getBody().get("token");
	}

	public static String adminToken(TestRestTemplate restTemplate) {
		return loginToken(restTemplate, DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
	}

	public static HttpHeaders bearer(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		return headers;
	}

	public static HttpHeaders basic(String apiKey, String apiSecret) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(apiKey, apiSecret);
		return headers;
	}

	public static ResponseEntity<Map<String, Object>> changeOwnPassword(TestRestTemplate restTemplate, String token,
			String currentPassword, String newPassword) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("currentPassword", currentPassword);
		payload.put("newPassword", newPassword);
		return jsonExchange(restTemplate, "/api/users/me/password", HttpMethod.POST, bearer(token), payload);
	}

	// ---- service registry -------------------------------------------------

	public static ResponseEntity<Map<String, Object>> registerService(TestRestTemplate restTemplate,
			String adminToken, String name) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("name", name);
		return jsonExchange(restTemplate, "/api/services", HttpMethod.POST, bearer(adminToken), payload);
	}

	/** Registers a fresh, uniquely-named service as ADMIN and returns its one-time credentials. */
	public static Map<String, Object> registerNewService(TestRestTemplate restTemplate, String adminToken,
			String namePrefix) {
		ResponseEntity<Map<String, Object>> response = registerService(restTemplate, adminToken,
				uniqueName(namePrefix));
		if (response.getStatusCode() != HttpStatus.CREATED || response.getBody() == null) {
			throw new IllegalStateException(
					"Expected 201 registering a service but got " + response.getStatusCode() + " with body "
							+ response.getBody());
		}
		return response.getBody();
	}

	public static ResponseEntity<List<Map<String, Object>>> listServices(TestRestTemplate restTemplate,
			HttpHeaders authHeaders) {
		HttpEntity<Void> request = new HttpEntity<>(null, authHeaders);
		return restTemplate.exchange("/api/services", HttpMethod.GET, request, LIST_TYPE);
	}

	public static ResponseEntity<Map<String, Object>> rotateCredentials(TestRestTemplate restTemplate, String id,
			HttpHeaders authHeaders) {
		HttpEntity<Void> request = new HttpEntity<>(null, authHeaders);
		return restTemplate.exchange("/api/services/" + id + "/credentials", HttpMethod.POST, request, MAP_TYPE);
	}

	// ---- tasks -------------------------------------------------

	public static Map<String, Object> minimalTaskPayload(String name, String script) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("name", name);
		payload.put("script", script);
		return payload;
	}

	public static ResponseEntity<Map<String, Object>> createTask(TestRestTemplate restTemplate,
			HttpHeaders authHeaders, Map<String, Object> payload) {
		return jsonExchange(restTemplate, "/api/tasks", HttpMethod.POST, authHeaders, payload);
	}

	public static ResponseEntity<Map<String, Object>> updateTask(TestRestTemplate restTemplate, String id,
			HttpHeaders authHeaders, Map<String, Object> payload) {
		return jsonExchange(restTemplate, "/api/tasks/" + id, HttpMethod.PUT, authHeaders, payload);
	}

	public static ResponseEntity<Map<String, Object>> getTask(TestRestTemplate restTemplate, String id,
			HttpHeaders authHeaders) {
		HttpEntity<Void> request = new HttpEntity<>(null, authHeaders);
		return restTemplate.exchange("/api/tasks/" + id, HttpMethod.GET, request, MAP_TYPE);
	}

	public static ResponseEntity<List<Map<String, Object>>> listTasks(TestRestTemplate restTemplate,
			HttpHeaders authHeaders) {
		HttpEntity<Void> request = new HttpEntity<>(null, authHeaders);
		return restTemplate.exchange("/api/tasks", HttpMethod.GET, request, LIST_TYPE);
	}

	// ---- plumbing -------------------------------------------------

	public static ResponseEntity<Map<String, Object>> jsonExchange(TestRestTemplate restTemplate, String url,
			HttpMethod method, HttpHeaders authHeaders, Map<String, Object> payload) {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll(authHeaders);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
		return restTemplate.exchange(url, method, request, MAP_TYPE);
	}

}
