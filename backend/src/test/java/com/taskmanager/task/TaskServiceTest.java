package com.taskmanager.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	private TaskRepository repository;

	private final Instant fixedNow = Instant.parse("2026-08-19T10:15:30Z");

	private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

	private TaskService service() {
		return new TaskService(repository, clock);
	}

	private TaskFields minimalFields(String name) {
		return new TaskFields(name, "acceptance-service", null, TaskStatus.CREATED, "/opt/scripts/acceptance.sh",
				null, null, false);
	}

	@Test
	void createGeneratesIdAndSetsCreationDateEqualToModificationDate() {
		when(repository.existsByNameIgnoreCase("Nightly backup")).thenReturn(false);
		when(repository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task created = service().create(minimalFields("Nightly backup"));

		assertThat(created.getId()).isNotNull();
		assertThat(created.getCreationDate()).isEqualTo(fixedNow);
		assertThat(created.getModificationDate()).isEqualTo(fixedNow);
		assertThat(created.getStatus()).isEqualTo(TaskStatus.CREATED);
		assertThat(created.isScheduled()).isFalse();
	}

	@Test
	void createThrowsDuplicateWhenNameAlreadyExistsCaseInsensitively() {
		when(repository.existsByNameIgnoreCase("backup")).thenReturn(true);

		assertThatThrownBy(() -> service().create(minimalFields("backup")))
			.isInstanceOf(DuplicateTaskNameException.class);

		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void createReportsDuplicateWhenTheUniqueIndexRejectsANameThatSlippedPastThePreCheck() {
		when(repository.existsByNameIgnoreCase("Nightly backup")).thenReturn(false);
		when(repository.saveAndFlush(any(Task.class))).thenThrow(uniqueViolation());

		assertThatThrownBy(() -> service().create(minimalFields("Nightly backup")))
			.isInstanceOf(DuplicateTaskNameException.class)
			.hasMessage("A task with name 'Nightly backup' already exists");
	}

	@Test
	void createPropagatesIntegrityViolationsThatAreNotUniqueNameConflicts() {
		when(repository.existsByNameIgnoreCase("Nightly backup")).thenReturn(false);
		DataIntegrityViolationException other = new DataIntegrityViolationException("not null violation",
				new SQLException("null value in column", "23502"));
		when(repository.saveAndFlush(any(Task.class))).thenThrow(other);

		assertThatThrownBy(() -> service().create(minimalFields("Nightly backup"))).isSameAs(other);
	}

	@Test
	void updateThrowsNotFoundWhenIdIsUnknown() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().update(id, minimalFields("New name")))
			.isInstanceOf(TaskNotFoundException.class)
			.hasMessage("Task " + id + " not found");
	}

	@Test
	void updatePreservesCreationDateBumpsModificationDateAndAppliesFields() {
		Instant createdAt = Instant.parse("2026-08-18T08:00:00Z");
		Task existing = Task.create("Original name", "svc", null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, createdAt);
		UUID id = existing.getId();
		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(repository.findByNameIgnoreCase("Original name")).thenReturn(Optional.of(existing));
		when(repository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TaskFields updateFields = new TaskFields("Original name", "svc", "updated description",
				TaskStatus.COMPLETED, "/opt/scripts/b.sh", null, null, false);

		Task updated = service().update(id, updateFields);

		assertThat(updated.getCreationDate()).isEqualTo(createdAt);
		assertThat(updated.getModificationDate()).isEqualTo(fixedNow);
		assertThat(updated.getStatus()).isEqualTo(TaskStatus.COMPLETED);
		assertThat(updated.getScript()).isEqualTo("/opt/scripts/b.sh");
		assertThat(updated.getDescription()).isEqualTo("updated description");
	}

	@Test
	void updateThrowsDuplicateWhenNameConflictsWithAnotherTask() {
		Task existing = Task.create("Task A", "svc", null, TaskStatus.CREATED, "/opt/scripts/a.sh", null, null,
				false, fixedNow);
		Task other = Task.create("Task B", "svc", null, TaskStatus.CREATED, "/opt/scripts/b.sh", null, null, false,
				fixedNow);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
		when(repository.findByNameIgnoreCase("Task B")).thenReturn(Optional.of(other));

		assertThatThrownBy(() -> service().update(existing.getId(), minimalFields("Task B")))
			.isInstanceOf(DuplicateTaskNameException.class);

		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void updateDoesNotTreatSameTaskKeepingItsOwnNameAsDuplicate() {
		Task existing = Task.create("Task A", "svc", null, TaskStatus.CREATED, "/opt/scripts/a.sh", null, null,
				false, fixedNow);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
		when(repository.findByNameIgnoreCase("Task A")).thenReturn(Optional.of(existing));
		when(repository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task updated = service().update(existing.getId(), minimalFields("Task A"));

		assertThat(updated.getName()).isEqualTo("Task A");
	}

	@Test
	void getByIdThrowsNotFoundWhenMissing() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().getById(id)).isInstanceOf(TaskNotFoundException.class);
	}

	@Test
	void getByIdReturnsTaskWhenPresent() {
		Task existing = Task.create("Task A", "svc", null, TaskStatus.CREATED, "/opt/scripts/a.sh", null, null,
				false, fixedNow);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

		assertThat(service().getById(existing.getId())).isEqualTo(existing);
	}

	@Test
	void listAllReturnsRepositoryResultOrderedByCreationDateDescending() {
		List<Task> tasks = List.of(mock(Task.class), mock(Task.class));
		when(repository.findAllByOrderByCreationDateDesc()).thenReturn(tasks);

		assertThat(service().listAll()).isEqualTo(tasks);
	}

	/** What the ux_tasks_name_ci index raises: SQL state 23505, unique violation. */
	private DataIntegrityViolationException uniqueViolation() {
		return new DataIntegrityViolationException("could not execute statement",
				new SQLException("duplicate key value violates unique constraint \"ux_tasks_name_ci\"", "23505"));
	}

}
