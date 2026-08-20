package com.taskmanager.acceptance;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.taskmanager.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static com.taskmanager.acceptance.support.AcceptanceTestSupport.adminToken;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.basic;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.bearer;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.jsonExchange;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.listServices;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.registerNewService;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.registerService;
import static com.taskmanager.acceptance.support.AcceptanceTestSupport.uniqueName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * API-level acceptance test for the first acceptance criterion of
 * {@code specs/service-registry-and-task-scoping.md}:
 *
 * "Given an ADMIN JWT, when POST /api/services with a name, then 201 with non-empty
 * apiKey and apiSecret, a subsequent GET /api/services returns the service without
 * either value, and neither the captured log output nor any later response contains
 * the raw apiSecret; given service A's credentials for the same call, then 403."
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class ServiceRegistryAcceptanceTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private ListAppender<ILoggingEvent> logCapture;

	@BeforeEach
	void captureLogs() {
		logCapture = new ListAppender<>();
		logCapture.start();
		rootLogger().addAppender(logCapture);
	}

	@AfterEach
	void stopCapturingLogs() {
		rootLogger().detachAppender(logCapture);
	}

	private Logger rootLogger() {
		return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	}

	@Test
	void adminRegisteringAServiceGetsOneTimeCredentialsNeverExposedAgainNorLogged() {
		String admin = adminToken(restTemplate);
		String name = uniqueName("backup-service");

		ResponseEntity<Map<String, Object>> response = registerService(restTemplate, admin, name);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Map<String, Object> body = response.getBody();
		assertThat(body).isNotNull();
		String apiKey = (String) body.get("apiKey");
		String apiSecret = (String) body.get("apiSecret");
		assertThat(apiKey).isNotBlank();
		assertThat(apiSecret).isNotBlank();
		assertThat(body.get("name")).isEqualTo(name);

		ResponseEntity<List<Map<String, Object>>> listResponse = listServices(restTemplate, bearer(admin));
		assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(listResponse.getBody()).isNotNull();
		Map<String, Object> listed = listResponse.getBody().stream()
			.filter(service -> name.equals(service.get("name")))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Registered service '" + name + "' not found in GET /api/services"));
		assertThat(listed).doesNotContainKey("apiKey");
		assertThat(listed).doesNotContainKey("apiSecret");
		assertThat(listResponse.getBody().toString()).doesNotContain(apiSecret);

		List<String> loggedMessages = logCapture.list.stream()
			.map(ILoggingEvent::getFormattedMessage)
			.filter(message -> message != null)
			.toList();
		assertThat(loggedMessages).noneMatch(message -> message.contains(apiSecret));
	}

	@Test
	void serviceCredentialsAttemptingToRegisterAServiceGet403() {
		String admin = adminToken(restTemplate);
		Map<String, Object> existing = registerNewService(restTemplate, admin, "caller-service");
		String apiKey = (String) existing.get("apiKey");
		String apiSecret = (String) existing.get("apiSecret");

		ResponseEntity<Map<String, Object>> response = jsonExchange(restTemplate, "/api/services", HttpMethod.POST,
				basic(apiKey, apiSecret), Map.of("name", uniqueName("another-service")));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

}
