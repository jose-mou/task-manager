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

import static com.taskmanager.acceptance.support.AcceptanceTestSupport.adminToken;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.basic;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.createTask;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.minimalTaskPayload;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.registerNewService;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.rotateCredentials;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * API-level acceptance test for the second acceptance criterion of
 * {@code specs/service-registry-and-task-scoping.md}:
 *
 * "Given a registered service, when POST /api/services/{id}/credentials with its own
 * credentials, then 200 with a different pair and the previous secret is refused with
 * 401 on the next task write."
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class ServiceCredentialRotationAcceptanceTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void rotatingOwnCredentialsReturnsADifferentPairAndInvalidatesThePreviousSecret() {
		String admin = adminToken(restTemplate);
		Map<String, Object> registered = registerNewService(restTemplate, admin, "rotating-service");
		String id = (String) registered.get("id");
		String originalApiKey = (String) registered.get("apiKey");
		String originalApiSecret = (String) registered.get("apiSecret");

		ResponseEntity<Map<String, Object>> rotateResponse = rotateCredentials(restTemplate, id,
				basic(originalApiKey, originalApiSecret));

		assertThat(rotateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, Object> rotated = rotateResponse.getBody();
		assertThat(rotated).isNotNull();
		String newApiKey = (String) rotated.get("apiKey");
		String newApiSecret = (String) rotated.get("apiSecret");
		assertThat(newApiKey).isNotBlank();
		assertThat(newApiSecret).isNotBlank();
		assertThat(newApiSecret).isNotEqualTo(originalApiSecret);

		// The previous secret must be refused on the very next task write.
		ResponseEntity<Map<String, Object>> writeWithStaleSecret = createTask(restTemplate,
				basic(originalApiKey, originalApiSecret), minimalTaskPayload(uniqueName("Stale write"), "/opt/scripts/stale.sh"));

		assertThat(writeWithStaleSecret.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		// The new pair authenticates a task write.
		ResponseEntity<Map<String, Object>> writeWithNewSecret = createTask(restTemplate,
				basic(newApiKey, newApiSecret), minimalTaskPayload(uniqueName("Fresh write"), "/opt/scripts/fresh.sh"));

		assertThat(writeWithNewSecret.getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}

}
