package com.taskmanager.task;

import com.taskmanager.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Slice test proving the Flyway-managed {@code tasks} table and the derived
 * queries {@link TaskRepository} relies on behave correctly against a real
 * PostgreSQL instance, in particular the case-insensitive uniqueness of
 * {@code name} enforced by the database (specs/task-management.md, rule 10).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestcontainersConfiguration.class)
class TaskRepositoryTest {

	@Autowired
	private TaskRepository repository;

	private Task newTask(String name) {
		return Task.create(name, "svc", null, TaskStatus.CREATED, "/opt/scripts/a.sh", null, null, false,
				Instant.now());
	}

	@Test
	void existsByNameIgnoreCaseMatchesRegardlessOfCase() {
		repository.saveAndFlush(newTask("Nightly Backup"));

		assertThat(repository.existsByNameIgnoreCase("nightly backup")).isTrue();
		assertThat(repository.existsByNameIgnoreCase("NIGHTLY BACKUP")).isTrue();
		assertThat(repository.existsByNameIgnoreCase("something else")).isFalse();
	}

	@Test
	void findByNameIgnoreCaseReturnsTheMatchingTask() {
		Task saved = repository.saveAndFlush(newTask("Nightly Backup"));

		assertThat(repository.findByNameIgnoreCase("nightly backup")).map(Task::getId).contains(saved.getId());
	}

	@Test
	void databaseRejectsInsertingASecondTaskWithNameDifferingOnlyByCase() {
		repository.saveAndFlush(newTask("Backup"));

		assertThatThrownBy(() -> repository.saveAndFlush(newTask("backup")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findAllByOrderByCreationDateDescReturnsNewestFirst() {
		Instant t0 = Instant.parse("2026-08-18T08:00:00Z");
		repository.saveAndFlush(Task.create("Oldest", "svc", null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, t0));
		repository.saveAndFlush(Task.create("Newest", "svc", null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, t0.plusSeconds(60)));
		repository.saveAndFlush(Task.create("Middle", "svc", null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, t0.plusSeconds(30)));

		List<String> names = repository.findAllByOrderByCreationDateDesc().stream().map(Task::getName).toList();

		assertThat(names).containsExactly("Newest", "Middle", "Oldest");
	}

}
