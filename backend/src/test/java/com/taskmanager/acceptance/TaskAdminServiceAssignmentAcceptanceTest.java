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

import java.util.List;
import java.util.Map;

import static com.taskmanager.acceptance.support.AcceptanceTestSupport.adminToken;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.bearer;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.createTask;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.minimalTaskPayload;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.registerNewService;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * API-level acceptance test for the eighth acceptance criterion of
 * {@code specs/service-registry-and-task-scoping.md}:
 *
 * "Given an ADMIN JWT, when POST /api/tasks with service naming a registered service,
 * then 201 owned by that service; when service names an unregistered one, then 400
 * naming service."
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class TaskAdminServiceAssignmentAcceptanceTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void adminCreatingATaskForARegisteredServiceReturns201OwnedByIt() {
		String admin = adminToken(restTemplate);
		Map<String, Object> service = registerNewService(restTemplate, admin, "admin-assigned-service");
		String serviceName = (String) service.get("name");
		HttpHeaders adminHeaders = bearer(admin);

		Map<String, Object> payload = minimalTaskPayload(uniqueName("Admin assigned task"), "/opt/scripts/admin.sh");
		payload.put("service", serviceName);

		ResponseEntity<Map<String, Object>> response = createTask(restTemplate, adminHeaders, payload);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody().get("service")).isEqualTo(serviceName);
	}

	@Test
	void adminCreatingATaskForAnUnregisteredServiceReturns400NamingServiceField() {
		String admin = adminToken(restTemplate);
		HttpHeaders adminHeaders = bearer(admin);

		Map<String, Object> payload = minimalTaskPayload(uniqueName("Admin unregistered service task"), "/opt/scripts/admin.sh");
		payload.put("service", uniqueName("never-registered-service"));

		ResponseEntity<Map<String, Object>> response = createTask(restTemplate, adminHeaders, payload);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		Map<String, Object> body = response.getBody();
		assertThat(body).isNotNull();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> errors = (List<Map<String, Object>>) body.get("errors");
		assertThat(errors).extracting(error -> error.get("field")).contains("service");
	}

}
