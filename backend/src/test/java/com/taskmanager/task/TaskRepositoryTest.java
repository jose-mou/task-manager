package com.taskmanager.task;

import com.taskmanager.TestcontainersConfiguration;
import com.taskmanager.service.ServiceAccount;
import com.taskmanager.service.ServiceAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Slice test proving the Flyway-managed {@code tasks} table and the derived
 * queries {@link TaskRepository} relies on behave correctly against a real
 * PostgreSQL instance, in particular the per-owning-service case-insensitive
 * uniqueness of {@code name} enforced by the database
 * (specs/service-registry-and-task-scoping.md, rule 10).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestcontainersConfiguration.class)
class TaskRepositoryTest {

	@Autowired
	private TaskRepository repository;

	@Autowired
	private ServiceAccountRepository serviceAccountRepository;

	private ServiceAccount serviceA;

	private ServiceAccount serviceB;

	@BeforeEach
	void registerServices() {
		serviceA = serviceAccountRepository
			.saveAndFlush(ServiceAccount.register("service-a-" + UUID.randomUUID(), "key-a", "hash-a", Instant.now()));
		serviceB = serviceAccountRepository
			.saveAndFlush(ServiceAccount.register("service-b-" + UUID.randomUUID(), "key-b", "hash-b", Instant.now()));
	}

	private Task newTask(String name, ServiceAccount owningService) {
		return Task.create(name, owningService, null, TaskStatus.CREATED, "/opt/scripts/a.sh", null, null, false,
				Instant.now());
	}

	@Test
	void existsByNameIgnoreCaseAndOwningServiceIdMatchesRegardlessOfCaseWithinTheSameService() {
		repository.saveAndFlush(newTask("Nightly Backup", serviceA));

		assertThat(repository.existsByNameIgnoreCaseAndOwningService_Id("nightly backup", serviceA.getId())).isTrue();
		assertThat(repository.existsByNameIgnoreCaseAndOwningService_Id("NIGHTLY BACKUP", serviceA.getId())).isTrue();
		assertThat(repository.existsByNameIgnoreCaseAndOwningService_Id("something else", serviceA.getId())).isFalse();
		assertThat(repository.existsByNameIgnoreCaseAndOwningService_Id("nightly backup", serviceB.getId())).isFalse();
	}

	@Test
	void findByNameIgnoreCaseAndOwningServiceIdReturnsTheMatchingTask() {
		Task saved = repository.saveAndFlush(newTask("Nightly Backup", serviceA));

		assertThat(repository.findByNameIgnoreCaseAndOwningService_Id("nightly backup", serviceA.getId()))
			.map(Task::getId)
			.contains(saved.getId());
	}

	@Test
	void databaseRejectsInsertingASecondTaskWithNameDifferingOnlyByCaseForTheSameService() {
		repository.saveAndFlush(newTask("Backup", serviceA));

		assertThatThrownBy(() -> repository.saveAndFlush(newTask("backup", serviceA)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseAllowsTheSameNameForTwoDifferentServices() {
		repository.saveAndFlush(newTask("Backup", serviceA));

		Task savedForB = repository.saveAndFlush(newTask("backup", serviceB));

		assertThat(savedForB.getId()).isNotNull();
	}

	@Test
	void findAllByOrderByCreationDateDescReturnsNewestFirst() {
		Instant t0 = Instant.parse("2026-08-18T08:00:00Z");
		repository.saveAndFlush(
				Task.create("Oldest", serviceA, null, TaskStatus.CREATED, "/opt/scripts/a.sh", null, null, false, t0));
		repository.saveAndFlush(Task.create("Newest", serviceA, null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, t0.plusSeconds(60)));
		repository.saveAndFlush(Task.create("Middle", serviceA, null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, t0.plusSeconds(30)));

		List<String> names = repository.findAllByOrderByCreationDateDesc().stream().map(Task::getName).toList();

		assertThat(names).containsExactly("Newest", "Middle", "Oldest");
	}

	@Test
	void findByOwningServiceNameOrderByCreationDateDescReturnsOnlyThatServicesTasks() {
		repository.saveAndFlush(newTask("Owned by A", serviceA));
		repository.saveAndFlush(newTask("Owned by B", serviceB));

		List<String> names = repository.findByOwningService_NameOrderByCreationDateDesc(serviceA.getName())
			.stream()
			.map(Task::getName)
			.toList();

		assertThat(names).containsExactly("Owned by A");
	}

}
