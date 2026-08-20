package com.taskmanager.acceptance;

import com.taskmanager.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static com.taskmanager.acceptance.support.AcceptanceTestSupport.adminToken;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.basic;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.createTask;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.getTask;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.minimalTaskPayload;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.registerNewService;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.updateTask;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * API-level acceptance tests for the sixth and seventh acceptance criteria of
 * {@code specs/service-registry-and-task-scoping.md}:
 *
 * "Given service A's credentials, when POST /api/tasks with a payload whose service
 * says 'B', then 201 and the created task's service is A."
 *
 * "Given a task owned by service B, when service A's credentials PUT it, then 403 and
 * the task is unchanged."
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class TaskServiceOwnershipAcceptanceTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private HttpHeaders serviceCredentials(String apiKey, String apiSecret) {
		return basic(apiKey, apiSecret);
	}

	@Test
	void serviceCredentialsIgnoreTheServiceFieldInThePayloadAndOwnershipFollowsTheCredentials() {
		String admin = adminToken(restTemplate);
		Map<String, Object> serviceA = registerNewService(restTemplate, admin, "service-a");
		Map<String, Object> serviceB = registerNewService(restTemplate, admin, "service-b");
		String serviceAName = (String) serviceA.get("name");
		String serviceBName = (String) serviceB.get("name");
		HttpHeaders aCredentials = serviceCredentials((String) serviceA.get("apiKey"), (String) serviceA.get("apiSecret"));

		Map<String, Object> payload = minimalTaskPayload(uniqueName("Owned by A despite payload"), "/opt/scripts/a.sh");
		payload.put("service", serviceBName);

		ResponseEntity<Map<String, Object>> response = createTask(restTemplate, aCredentials, payload);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody().get("service")).isEqualTo(serviceAName);
	}

	@Test
	void serviceCredentialsUpdatingATaskOwnedByAnotherServiceGet403AndTheTaskIsUnchanged() {
		String admin = adminToken(restTemplate);
		Map<String, Object> serviceA = registerNewService(restTemplate, admin, "service-a-updater");
		Map<String, Object> serviceB = registerNewService(restTemplate, admin, "service-b-owner");
		HttpHeaders aCredentials = serviceCredentials((String) serviceA.get("apiKey"), (String) serviceA.get("apiSecret"));
		HttpHeaders bCredentials = serviceCredentials((String) serviceB.get("apiKey"), (String) serviceB.get("apiSecret"));

		ResponseEntity<Map<String, Object>> created = createTask(restTemplate, bCredentials,
				minimalTaskPayload(uniqueName("Owned by B"), "/opt/scripts/b.sh"));
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String id = (String) created.getBody().get("id");
		Map<String, Object> beforeAttempt = created.getBody();

		Map<String, Object> maliciousUpdate = minimalTaskPayload((String) beforeAttempt.get("name"), "/opt/scripts/hijacked.sh");
		maliciousUpdate.put("description", "hijack attempt");

		ResponseEntity<Map<String, Object>> updateResponse = updateTask(restTemplate, id, aCredentials, maliciousUpdate);

		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<Map<String, Object>> afterAttempt = getTask(restTemplate, id, new HttpHeaders());
		assertThat(afterAttempt.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(afterAttempt.getBody().get("script")).isEqualTo(beforeAttempt.get("script"));
		assertThat(afterAttempt.getBody().get("description")).isEqualTo(beforeAttempt.get("description"));
		assertThat(afterAttempt.getBody().get("modificationDate")).isEqualTo(beforeAttempt.get("modificationDate"));
	}

}
