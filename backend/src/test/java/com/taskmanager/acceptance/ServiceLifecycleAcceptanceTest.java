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
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.bearer;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.changeOwnPassword;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.createTask;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.deleteService;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.getTask;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.jsonExchange;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.minimalTaskPayload;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.registerNewService;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.renameService;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * API-level acceptance test for the parts of the service registry that the
 * per-criterion classes do not reach: the rename and delete operations of
 * {@code specs/service-registry-and-task-scoping.md}'s API impact section, and
 * behaviour rule 2's "403 for another service's credentials" at the HTTP level.
 *
 * Everything asserted here is declared in api/openapi.yaml:
 * {@code PUT /api/services/{id}} is allowed to an ADMIN JWT or that same service's
 * own credentials; {@code DELETE /api/services/{id}} is ADMIN-only "even for the
 * service being deleted" and "deletes every task it owns"; and
 * {@code POST /api/users/me/password} rejects service credentials with 403.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class ServiceLifecycleAcceptanceTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void aServiceCanRenameItselfAndItsTasksFollowTheNewName() {
		String admin = adminToken(restTemplate);
		Map<String, Object> service = registerNewService(restTemplate, admin, "self-renaming-service");
		String id = (String) service.get("id");
		HttpHeaders ownCredentials = basic((String) service.get("apiKey"), (String) service.get("apiSecret"));
		ResponseEntity<Map<String, Object>> task = createTask(restTemplate, ownCredentials,
				minimalTaskPayload(uniqueName("Task of a renamed service"), "/opt/scripts/renamed.sh"));
		assertThat(task.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String newName = uniqueName("renamed-service");

		ResponseEntity<Map<String, Object>> renamed = renameService(restTemplate, id, ownCredentials, newName);

		assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(renamed.getBody()).isNotNull();
		assertThat(renamed.getBody().get("name")).isEqualTo(newName);
		assertThat(renamed.getBody()).doesNotContainKeys("apiKey", "apiSecret");

		ResponseEntity<Map<String, Object>> reread = getTask(restTemplate, (String) task.getBody().get("id"),
				new HttpHeaders());
		assertThat(reread.getBody().get("service")).isEqualTo(newName);
	}

	@Test
	void anotherServicesCredentialsCannotRenameAService() {
		String admin = adminToken(restTemplate);
		Map<String, Object> target = registerNewService(restTemplate, admin, "rename-target-service");
		Map<String, Object> intruder = registerNewService(restTemplate, admin, "rename-intruder-service");
		String targetId = (String) target.get("id");
		HttpHeaders intruderCredentials = basic((String) intruder.get("apiKey"), (String) intruder.get("apiSecret"));

		ResponseEntity<Map<String, Object>> response = renameService(restTemplate, targetId, intruderCredentials,
				uniqueName("hijacked-name"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		// The targeted resource is left unchanged (api/openapi.yaml, Forbidden).
		ResponseEntity<Map<String, Object>> stillNamed = renameService(restTemplate, targetId, bearer(admin),
				(String) target.get("name"));
		assertThat(stillNamed.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(stillNamed.getBody().get("name")).isEqualTo(target.get("name"));
	}

	@Test
	void deletingAServiceDeletesItsTasksAndItsCredentialsStopAuthenticating() {
		String admin = adminToken(restTemplate);
		Map<String, Object> service = registerNewService(restTemplate, admin, "deletable-service");
		String id = (String) service.get("id");
		HttpHeaders credentials = basic((String) service.get("apiKey"), (String) service.get("apiSecret"));
		ResponseEntity<Map<String, Object>> task = createTask(restTemplate, credentials,
				minimalTaskPayload(uniqueName("Task of a deleted service"), "/opt/scripts/doomed.sh"));
		assertThat(task.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String taskId = (String) task.getBody().get("id");

		ResponseEntity<Map<String, Object>> deleted = deleteService(restTemplate, id, bearer(admin));

		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(getTask(restTemplate, taskId, new HttpHeaders()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(createTask(restTemplate, credentials,
				minimalTaskPayload(uniqueName("After deletion"), "/opt/scripts/after.sh")).getStatusCode())
			.isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(deleteService(restTemplate, id, bearer(admin)).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void serviceCredentialsCannotDeleteAServiceNotEvenTheirOwn() {
		String admin = adminToken(restTemplate);
		Map<String, Object> service = registerNewService(restTemplate, admin, "self-deleting-service");
		String id = (String) service.get("id");
		HttpHeaders ownCredentials = basic((String) service.get("apiKey"), (String) service.get("apiSecret"));

		ResponseEntity<Map<String, Object>> response = deleteService(restTemplate, id, ownCredentials);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void serviceCredentialsCannotListTheRegistryNorChangeAPassword() {
		String admin = adminToken(restTemplate);
		Map<String, Object> service = registerNewService(restTemplate, admin, "no-registry-access-service");
		HttpHeaders credentials = basic((String) service.get("apiKey"), (String) service.get("apiSecret"));

		ResponseEntity<Map<String, Object>> listing = jsonExchange(restTemplate, "/api/services",
				org.springframework.http.HttpMethod.GET, credentials, null);
		assertThat(listing.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<Map<String, Object>> passwordChange = jsonExchange(restTemplate, "/api/users/me/password",
				org.springframework.http.HttpMethod.POST, credentials,
				Map.of("currentPassword", "admin", "newPassword", "a-brand-new-valid-password"));
		assertThat(passwordChange.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void anAnonymousCallerCannotReachTheServiceRegistry() {
		ResponseEntity<Map<String, Object>> response = jsonExchange(restTemplate, "/api/services",
				org.springframework.http.HttpMethod.POST, new HttpHeaders(),
				Map.of("name", uniqueName("anonymous-service")));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void changingAPasswordIsRejectedWithoutAnyAuthentication() {
		ResponseEntity<Map<String, Object>> response = changeOwnPassword(restTemplate, "not-a-token", "admin",
				"a-brand-new-valid-password");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
