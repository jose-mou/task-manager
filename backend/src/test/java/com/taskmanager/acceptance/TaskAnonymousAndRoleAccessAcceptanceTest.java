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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.taskmanager.acceptance.support.AcceptanceTestSupport.adminToken;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.basic;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.bearer;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.createTask;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.getTask;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.listTasks;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.loginToken;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.minimalTaskPayload;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.registerNewService;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.uniqueName;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.updateTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * API-level acceptance test for the fifth acceptance criterion of
 * {@code specs/service-registry-and-task-scoping.md}:
 *
 * "Given no Authorization header, when GET /api/tasks and GET /api/tasks/{id} for a
 * task owned by any service, then 200 on both; when POST /api/tasks, then 401; and
 * with a USER-role JWT, then 403."
 *
 * NOTE on the USER-role fixture: the spec explicitly puts "user self-registration and
 * user CRUD endpoints" out of scope, so there is no HTTP operation that can create a
 * USER-role account. The only way to obtain a USER-role JWT to exercise this criterion
 * is to insert the account directly with JDBC, bypassing the API for *setup* only (every
 * assertion below still goes exclusively through HTTP). This directly seeds the `users`
 * table using the columns implied by the spec's field list (id, username, passwordHash,
 * role, creationDate) in the snake_case convention already used by V1__create_tasks_table.sql;
 * see the final report for this gap called out as a contract ambiguity.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class TaskAnonymousAndRoleAccessAcceptanceTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final HttpHeaders ANONYMOUS = new HttpHeaders();

	private String seedUserAccount(String username, String rawPassword) {
		String hash = new BCryptPasswordEncoder().encode(rawPassword);
		jdbcTemplate.update(
				"INSERT INTO users (id, username, password_hash, role, creation_date) VALUES (?, ?, ?, ?, ?)",
				UUID.randomUUID(), username, hash, "USER", OffsetDateTime.now());
		return username;
	}

	@Test
	void anonymousCallersCanReadAnyTaskButNotCreateOne() {
		String admin = adminToken(restTemplate);
		Map<String, Object> service = registerNewService(restTemplate, admin, "readable-service");
		String apiKey = (String) service.get("apiKey");
		String apiSecret = (String) service.get("apiSecret");
		ResponseEntity<Map<String, Object>> created = createTask(restTemplate, basic(apiKey, apiSecret),
				minimalTaskPayload(uniqueName("Anonymous read target"), "/opt/scripts/read.sh"));
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String id = (String) created.getBody().get("id");

		ResponseEntity<List<Map<String, Object>>> listResponse = listTasks(restTemplate, ANONYMOUS);
		assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		ResponseEntity<Map<String, Object>> getResponse = getTask(restTemplate, id, ANONYMOUS);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getResponse.getBody().get("id")).isEqualTo(id);

		ResponseEntity<Map<String, Object>> anonymousCreate = createTask(restTemplate, ANONYMOUS,
				minimalTaskPayload(uniqueName("Anonymous write attempt"), "/opt/scripts/deny.sh"));
		assertThat(anonymousCreate.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void aUserRoleJwtIsForbiddenFromCreatingATask() {
		String username = uniqueName("plain-user");
		String rawPassword = "plain-user-password";
		seedUserAccount(username, rawPassword);
		String userToken = loginToken(restTemplate, username, rawPassword);

		ResponseEntity<Map<String, Object>> response = createTask(restTemplate, bearer(userToken),
				minimalTaskPayload(uniqueName("User role write attempt"), "/opt/scripts/deny-user.sh"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void aUserRoleJwtIsForbiddenFromUpdatingATaskAndAnAnonymousCallerIsUnauthorized() {
		String admin = adminToken(restTemplate);
		Map<String, Object> service = registerNewService(restTemplate, admin, "updatable-service");
		ResponseEntity<Map<String, Object>> created = createTask(restTemplate,
				basic((String) service.get("apiKey"), (String) service.get("apiSecret")),
				minimalTaskPayload(uniqueName("Update target"), "/opt/scripts/update.sh"));
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String id = (String) created.getBody().get("id");
		Map<String, Object> payload = minimalTaskPayload(uniqueName("Hijacked"), "/opt/scripts/hijack.sh");

		assertThat(updateTask(restTemplate, id, ANONYMOUS, payload).getStatusCode())
			.isEqualTo(HttpStatus.UNAUTHORIZED);

		String username = uniqueName("plain-updater");
		String rawPassword = "plain-updater-password";
		seedUserAccount(username, rawPassword);
		String userToken = loginToken(restTemplate, username, rawPassword);

		assertThat(updateTask(restTemplate, id, bearer(userToken), payload).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);
	}

	/**
	 * "Credentials are ignored when present, and are never required" on the anonymous
	 * reads (specs/service-registry-and-task-scoping.md rule 7, and the `security: []`
	 * of GET /api/tasks in api/openapi.yaml). A caller still holding a rotated-away
	 * apiSecret must therefore keep reading normally - only its *writes* are rejected
	 * with 401.
	 */
	@Test
	void readsIgnoreCredentialsThatNoLongerAuthenticate() {
		String admin = adminToken(restTemplate);
		Map<String, Object> service = registerNewService(restTemplate, admin, "stale-credentials-service");
		HttpHeaders staleCredentials = basic((String) service.get("apiKey"), "not-the-secret-anymore");
		ResponseEntity<Map<String, Object>> created = createTask(restTemplate,
				basic((String) service.get("apiKey"), (String) service.get("apiSecret")),
				minimalTaskPayload(uniqueName("Readable with stale credentials"), "/opt/scripts/stale-read.sh"));
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String id = (String) created.getBody().get("id");

		assertThat(listTasks(restTemplate, staleCredentials).getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getTask(restTemplate, id, staleCredentials).getStatusCode()).isEqualTo(HttpStatus.OK);

		ResponseEntity<Map<String, Object>> write = createTask(restTemplate, staleCredentials,
				minimalTaskPayload(uniqueName("Stale write attempt"), "/opt/scripts/stale-write.sh"));
		assertThat(write.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
