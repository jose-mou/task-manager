package com.taskmanager.acceptance;

import com.taskmanager.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * API-level acceptance tests for the task management feature (outer TDD loop).
 *
 * Each test is the executable form of one acceptance criterion from
 * {@code specs/task-management.md}, exercised strictly over HTTP against the
 * contract frozen in {@code api/openapi.yaml}. No production types are referenced:
 * requests and responses are handled as plain maps/lists so these tests fail only
 * because the endpoints do not exist yet, never because of a compile-time
 * dependency on unimplemented production code.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class TaskAcceptanceTest {

	private static final String TASKS_URL = "/api/tasks";

	@Autowired
	private TestRestTemplate restTemplate;

	// ---- helpers -----------------------------------------------------

	private Map<String, Object> minimalTaskPayload(String name) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", name);
		body.put("service", "acceptance-service");
		body.put("script", "/opt/scripts/acceptance.sh");
		return body;
	}

	private String uniqueName(String prefix) {
		return prefix + " " + UUID.randomUUID();
	}

	private ResponseEntity<Map<String, Object>> createTask(Map<String, Object> payload) {
		return exchangeForObject(TASKS_URL, HttpMethod.POST, payload);
	}

	private ResponseEntity<Map<String, Object>> updateTask(String id, Map<String, Object> payload) {
		return exchangeForObject(TASKS_URL + "/" + id, HttpMethod.PUT, payload);
	}

	private ResponseEntity<Map<String, Object>> exchangeForObject(String url, HttpMethod method,
			Map<String, Object> payload) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
		return restTemplate.exchange(url, method, request, new ParameterizedTypeReference<Map<String, Object>>() {
		});
	}

	private ResponseEntity<List<Map<String, Object>>> listTasks() {
		return restTemplate.exchange(TASKS_URL, HttpMethod.GET, null,
				new ParameterizedTypeReference<List<Map<String, Object>>>() {
				});
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> errorsOf(ResponseEntity<Map<String, Object>> response) {
		return (List<Map<String, Object>>) response.getBody().get("errors");
	}

	// ---- acceptance criteria ------------------------------------------

	@Test
	void creatingTaskWithOnlyRequiredFieldsReturns201WithGeneratedIdAndDefaults() {
		Map<String, Object> payload = minimalTaskPayload(uniqueName("Report generation"));

		ResponseEntity<Map<String, Object>> response = createTask(payload);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Map<String, Object> body = response.getBody();
		assertThat(body).isNotNull();
		assertThat((String) body.get("id")).isNotBlank();
		assertThat(UUID.fromString((String) body.get("id"))).isNotNull();
		assertThat(body.get("status")).isEqualTo("CREATED");
		assertThat(body.get("scheduled")).isEqualTo(false);
		assertThat(body.get("creationDate")).isEqualTo(body.get("modificationDate"));
	}

	@Test
	void creatingTaskWithBlankNameAndInvalidMaxExecutionsReturns400ListingBothFields() {
		Map<String, Object> payload = minimalTaskPayload("   ");
		payload.put("maxExecutions", 0);

		ResponseEntity<Map<String, Object>> response = createTask(payload);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		List<Object> offendingFields = errorsOf(response).stream().map(error -> error.get("field")).toList();
		assertThat(offendingFields).contains("name", "maxExecutions");
	}

	@Test
	void creatingScheduledTaskWithMissingOrInvalidCronExprReturns400NamingCronExprField() {
		Map<String, Object> missingCron = minimalTaskPayload(uniqueName("Missing cron"));
		missingCron.put("scheduled", true);

		ResponseEntity<Map<String, Object>> missingCronResponse = createTask(missingCron);

		assertThat(missingCronResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(errorsOf(missingCronResponse)).extracting(error -> error.get("field")).contains("cronExpr");

		Map<String, Object> invalidCron = minimalTaskPayload(uniqueName("Invalid cron"));
		invalidCron.put("scheduled", true);
		invalidCron.put("cronExpr", "not a valid cron expression");

		ResponseEntity<Map<String, Object>> invalidCronResponse = createTask(invalidCron);

		assertThat(invalidCronResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(errorsOf(invalidCronResponse)).extracting(error -> error.get("field")).contains("cronExpr");
	}

	@Test
	void creatingTaskWithUnknownStatusReturns400NamingStatusField() {
		Map<String, Object> payload = minimalTaskPayload(uniqueName("Unknown status"));
		payload.put("status", "PAUSED");

		ResponseEntity<Map<String, Object>> response = createTask(payload);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(errorsOf(response)).extracting(error -> error.get("field")).contains("status");
	}

	@Test
	void creatingTaskWithDuplicateNameCaseInsensitiveReturns409AndDoesNotCreateTask() {
		String baseName = uniqueName("Backup");
		ResponseEntity<Map<String, Object>> firstResponse = createTask(minimalTaskPayload(baseName));
		assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		ResponseEntity<Map<String, Object>> duplicateResponse = createTask(minimalTaskPayload(baseName.toLowerCase()));

		assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		long matchingTasks = listTasks().getBody().stream()
			.filter(task -> baseName.equalsIgnoreCase((String) task.get("name")))
			.count();
		assertThat(matchingTasks).isEqualTo(1);
	}

	@Test
	void updatingStoredTaskChangesFieldsPreservesCreationDateAndBumpsModificationDate() throws InterruptedException {
		Map<String, Object> createPayload = minimalTaskPayload(uniqueName("Nightly backup"));
		ResponseEntity<Map<String, Object>> createResponse = createTask(createPayload);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Map<String, Object> created = createResponse.getBody();
		String id = (String) created.get("id");
		String originalCreationDate = (String) created.get("creationDate");
		Instant originalModificationDate = Instant.parse((String) created.get("modificationDate"));
		assertThat((String) created.get("status")).isEqualTo("CREATED");

		Thread.sleep(50);

		Map<String, Object> updatePayload = minimalTaskPayload((String) createPayload.get("name"));
		updatePayload.put("status", "COMPLETED");
		updatePayload.put("script", "/opt/scripts/backup-v2.sh");
		updatePayload.put("description", "updated description");

		ResponseEntity<Map<String, Object>> updateResponse = updateTask(id, updatePayload);

		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, Object> updated = updateResponse.getBody();
		assertThat(updated.get("status")).isEqualTo("COMPLETED");
		assertThat(updated.get("script")).isEqualTo("/opt/scripts/backup-v2.sh");
		assertThat(updated.get("description")).isEqualTo("updated description");
		assertThat(updated.get("creationDate")).isEqualTo(originalCreationDate);
		assertThat(Instant.parse((String) updated.get("modificationDate"))).isAfter(originalModificationDate);
	}

	@Test
	void updatingTaskWithNonExistentIdReturns404() {
		String randomId = UUID.randomUUID().toString();

		ResponseEntity<Map<String, Object>> response = updateTask(randomId,
				minimalTaskPayload(uniqueName("Ghost task")));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		// The contract mandates the exact "Task {id} not found" message for this case
		// (see components.responses.TaskNotFound in api/openapi.yaml); asserting it here
		// keeps this test from passing vacuously against Spring's default 404 handler for
		// an unmapped route, which returns a generic body instead of this specific one.
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().get("message")).isEqualTo("Task " + randomId + " not found");
	}

	@Test
	void listingTasksReturnsThemOrderedByCreationDateDescending() throws InterruptedException {
		String suffix = UUID.randomUUID().toString();
		String firstName = "Order test A " + suffix;
		String secondName = "Order test B " + suffix;
		String thirdName = "Order test C " + suffix;

		assertThat(createTask(minimalTaskPayload(firstName)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Thread.sleep(20);
		assertThat(createTask(minimalTaskPayload(secondName)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Thread.sleep(20);
		assertThat(createTask(minimalTaskPayload(thirdName)).getStatusCode()).isEqualTo(HttpStatus.CREATED);

		ResponseEntity<List<Map<String, Object>>> listResponse = listTasks();

		assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<String> orderedRelevantNames = listResponse.getBody()
			.stream()
			.map(task -> (String) task.get("name"))
			.filter(name -> name.equals(firstName) || name.equals(secondName) || name.equals(thirdName))
			.toList();

		assertThat(orderedRelevantNames).containsExactly(thirdName, secondName, firstName);
	}

}
