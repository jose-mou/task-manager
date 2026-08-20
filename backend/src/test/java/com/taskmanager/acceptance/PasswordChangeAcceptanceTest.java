package com.taskmanager.acceptance;

import com.taskmanager.TestcontainersConfiguration;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static com.taskmanager.acceptance.support.AcceptanceTestSupport.DEFAULT_ADMIN_PASSWORD;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.DEFAULT_ADMIN_USERNAME;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.adminToken;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.changeOwnPassword;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * API-level acceptance test for the fourth acceptance criterion of
 * {@code specs/service-registry-and-task-scoping.md}:
 *
 * "Given the seeded admin authenticated, when POST /api/users/me/password with the
 * correct current password and a valid new one, then 204, a login with the old
 * password returns 401 and a login with the new password returns 200 with a JWT."
 *
 * This is the only acceptance test that mutates the seeded admin's password, which is a
 * singleton account shared by every other acceptance test class through the cached
 * Spring context / Testcontainers Postgres instance. {@code @DirtiesContext} forces a
 * brand-new context (and container) for whichever test runs next, so this permanent
 * mutation - the admin/admin default cannot be restored through the API because "admin"
 * is shorter than the required 8 characters - never leaks into unrelated tests.
 *
 * The same mutation is irreversible within this class too, so the methods are explicitly
 * ordered: the non-destructive wrong-password case runs first, while the seeded
 * admin/admin credentials still authenticate.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PasswordChangeAcceptanceTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	@Order(2)
	void changingOwnPasswordWithTheCorrectCurrentPasswordInvalidatesTheOldOneAndActivatesTheNewOne() {
		String token = adminToken(restTemplate);
		String newPassword = "sup3r-s3cret-pass";

		ResponseEntity<Map<String, Object>> changeResponse = changeOwnPassword(restTemplate, token,
				DEFAULT_ADMIN_PASSWORD, newPassword);

		assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<Map<String, Object>> loginWithOldPassword = login(restTemplate, DEFAULT_ADMIN_USERNAME,
				DEFAULT_ADMIN_PASSWORD);
		assertThat(loginWithOldPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		ResponseEntity<Map<String, Object>> loginWithNewPassword = login(restTemplate, DEFAULT_ADMIN_USERNAME,
				newPassword);
		assertThat(loginWithNewPassword.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(loginWithNewPassword.getBody()).isNotNull();
		assertThat((String) loginWithNewPassword.getBody().get("token")).isNotBlank();
	}

	@Test
	@Order(1)
	void changingOwnPasswordWithWrongCurrentPasswordReturns400NamingCurrentPassword() {
		String token = adminToken(restTemplate);

		ResponseEntity<Map<String, Object>> response = changeOwnPassword(restTemplate, token, "wrong-current-password",
				"a-brand-new-valid-password");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		Map<String, Object> body = response.getBody();
		assertThat(body).isNotNull();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> errors = (List<Map<String, Object>>) body.get("errors");
		assertThat(errors).extracting(error -> error.get("field")).contains("currentPassword");
	}

}
