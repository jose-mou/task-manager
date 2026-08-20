package com.taskmanager.acceptance;

import com.taskmanager.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static com.taskmanager.acceptance.support.AcceptanceTestSupport.DEFAULT_ADMIN_PASSWORD;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.DEFAULT_ADMIN_USERNAME;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * API-level acceptance test for the third acceptance criterion of
 * {@code specs/service-registry-and-task-scoping.md}:
 *
 * "Given a freshly started system with an empty users table, when POST /api/auth/login
 * with admin / admin, then 200 with a JWT."
 *
 * The seeding happens once, at application startup (behaviour rule 4 of the spec), which
 * for this Spring context is exactly the moment the shared Testcontainers-backed context
 * first comes up with an empty `users` table; this test simply proves the documented
 * default credentials work against that bootstrap.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class AuthenticationAcceptanceTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void loggingInWithTheSeededAdminDefaultCredentialsReturns200WithAJwt() {
		ResponseEntity<Map<String, Object>> response = login(restTemplate, DEFAULT_ADMIN_USERNAME,
				DEFAULT_ADMIN_PASSWORD);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, Object> body = response.getBody();
		assertThat(body).isNotNull();
		assertThat((String) body.get("token")).isNotBlank();
		assertThat(body.get("expiresAt")).isNotNull();
		assertThat(body.get("role")).isEqualTo("ADMIN");
	}

	@Test
	void loggingInWithWrongPasswordReturns401() {
		ResponseEntity<Map<String, Object>> response = login(restTemplate, DEFAULT_ADMIN_USERNAME,
				"definitely-not-the-password");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
