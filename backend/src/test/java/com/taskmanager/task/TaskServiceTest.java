package com.taskmanager.task;

import com.taskmanager.security.AdminCaller;
import com.taskmanager.security.ServiceCaller;
import com.taskmanager.service.ServiceAccount;
import com.taskmanager.service.ServiceAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

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

	@Mock
	private ServiceAccountRepository serviceAccountRepository;

	private final Instant fixedNow = Instant.parse("2026-08-19T10:15:30Z");

	private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

	private final ServiceAccount owningService = ServiceAccount.register("acceptance-service", "key", "hash",
			fixedNow);

	private final AdminCaller admin = new AdminCaller("admin");

	private TaskService service() {
		return new TaskService(repository, serviceAccountRepository, clock);
	}

	private TaskFields minimalFields(String name) {
		return new TaskFields(name, owningService.getId(), null, TaskStatus.CREATED, "/opt/scripts/acceptance.sh",
				null, null, false);
	}

	private void stubOwningService() {
		when(serviceAccountRepository.findById(owningService.getId())).thenReturn(Optional.of(owningService));
	}

	@Test
	void createGeneratesIdAndSetsCreationDateEqualToModificationDate() {
		stubOwningService();
		when(repository.existsByNameIgnoreCaseAndOwningService_Id("Nightly backup", owningService.getId()))
			.thenReturn(false);
		when(repository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task created = service().create(minimalFields("Nightly backup"));

		assertThat(created.getId()).isNotNull();
		assertThat(created.getCreationDate()).isEqualTo(fixedNow);
		assertThat(created.getModificationDate()).isEqualTo(fixedNow);
		assertThat(created.getStatus()).isEqualTo(TaskStatus.CREATED);
		assertThat(created.isScheduled()).isFalse();
		assertThat(created.getOwningService()).isEqualTo(owningService);
	}

	@Test
	void createThrowsDuplicateWhenNameAlreadyExistsCaseInsensitively() {
		stubOwningService();
		when(repository.existsByNameIgnoreCaseAndOwningService_Id("backup", owningService.getId())).thenReturn(true);

		assertThatThrownBy(() -> service().create(minimalFields("backup")))
			.isInstanceOf(DuplicateTaskNameException.class);

		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void createReportsDuplicateWhenTheUniqueIndexRejectsANameThatSlippedPastThePreCheck() {
		stubOwningService();
		when(repository.existsByNameIgnoreCaseAndOwningService_Id("Nightly backup", owningService.getId()))
			.thenReturn(false);
		when(repository.saveAndFlush(any(Task.class))).thenThrow(uniqueViolation());

		assertThatThrownBy(() -> service().create(minimalFields("Nightly backup")))
			.isInstanceOf(DuplicateTaskNameException.class)
			.hasMessage("A task with name 'Nightly backup' already exists for service 'acceptance-service'");
	}

	@Test
	void createPropagatesIntegrityViolationsThatAreNotUniqueNameConflicts() {
		stubOwningService();
		when(repository.existsByNameIgnoreCaseAndOwningService_Id("Nightly backup", owningService.getId()))
			.thenReturn(false);
		DataIntegrityViolationException other = new DataIntegrityViolationException("not null violation",
				new SQLException("null value in column", "23502"));
		when(repository.saveAndFlush(any(Task.class))).thenThrow(other);

		assertThatThrownBy(() -> service().create(minimalFields("Nightly backup"))).isSameAs(other);
	}

	@Test
	void updateThrowsNotFoundWhenIdIsUnknown() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().update(id, minimalFields("New name"), admin))
			.isInstanceOf(TaskNotFoundException.class)
			.hasMessage("Task " + id + " not found");
	}

	@Test
	void updatePreservesCreationDateBumpsModificationDateAndAppliesFields() {
		Instant createdAt = Instant.parse("2026-08-18T08:00:00Z");
		Task existing = Task.create("Original name", owningService, null, TaskStatus.CREATED, "/opt/scripts/a.sh",
				null, null, false, createdAt);
		UUID id = existing.getId();
		when(repository.findById(id)).thenReturn(Optional.of(existing));
		stubOwningService();
		when(repository.findByNameIgnoreCaseAndOwningService_Id("Original name", owningService.getId()))
			.thenReturn(Optional.of(existing));
		when(repository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TaskFields updateFields = new TaskFields("Original name", owningService.getId(), "updated description",
				TaskStatus.COMPLETED, "/opt/scripts/b.sh", null, null, false);

		Task updated = service().update(id, updateFields, admin);

		assertThat(updated.getCreationDate()).isEqualTo(createdAt);
		assertThat(updated.getModificationDate()).isEqualTo(fixedNow);
		assertThat(updated.getStatus()).isEqualTo(TaskStatus.COMPLETED);
		assertThat(updated.getScript()).isEqualTo("/opt/scripts/b.sh");
		assertThat(updated.getDescription()).isEqualTo("updated description");
	}

	@Test
	void updateThrowsDuplicateWhenNameConflictsWithAnotherTask() {
		Task existing = Task.create("Task A", owningService, null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, fixedNow);
		Task other = Task.create("Task B", owningService, null, TaskStatus.CREATED, "/opt/scripts/b.sh", null, null,
				false, fixedNow);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
		stubOwningService();
		when(repository.findByNameIgnoreCaseAndOwningService_Id("Task B", owningService.getId()))
			.thenReturn(Optional.of(other));

		assertThatThrownBy(() -> service().update(existing.getId(), minimalFields("Task B"), admin))
			.isInstanceOf(DuplicateTaskNameException.class);

		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void updateDoesNotTreatSameTaskKeepingItsOwnNameAsDuplicate() {
		Task existing = Task.create("Task A", owningService, null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, fixedNow);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
		stubOwningService();
		when(repository.findByNameIgnoreCaseAndOwningService_Id("Task A", owningService.getId()))
			.thenReturn(Optional.of(existing));
		when(repository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task updated = service().update(existing.getId(), minimalFields("Task A"), admin);

		assertThat(updated.getName()).isEqualTo("Task A");
	}

	@Test
	void updateRejectsAServiceCallerActingOnATaskOwnedByAnotherService() {
		Task existing = Task.create("Task A", owningService, null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, fixedNow);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
		ServiceCaller anotherService = new ServiceCaller(UUID.randomUUID(), "another-service");

		assertThatThrownBy(() -> service().update(existing.getId(), minimalFields("Task A"), anotherService))
			.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void updateAllowsAServiceCallerActingOnItsOwnTask() {
		Task existing = Task.create("Task A", owningService, null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, fixedNow);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
		stubOwningService();
		when(repository.findByNameIgnoreCaseAndOwningService_Id("Task A", owningService.getId()))
			.thenReturn(Optional.of(existing));
		when(repository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ServiceCaller ownCaller = new ServiceCaller(owningService.getId(), owningService.getName());

		Task updated = service().update(existing.getId(), minimalFields("Task A"), ownCaller);

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
		Task existing = Task.create("Task A", owningService, null, TaskStatus.CREATED, "/opt/scripts/a.sh", null,
				null, false, fixedNow);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

		assertThat(service().getById(existing.getId())).isEqualTo(existing);
	}

	@Test
	void listAllReturnsRepositoryResultOrderedByCreationDateDescendingWhenNoFilterIsGiven() {
		List<Task> tasks = List.of(mock(Task.class), mock(Task.class));
		when(repository.findAllByOrderByCreationDateDesc()).thenReturn(tasks);

		assertThat(service().listAll(null)).isEqualTo(tasks);
	}

	@Test
	void listAllFiltersByOwningServiceNameWhenGiven() {
		List<Task> tasks = List.of(mock(Task.class));
		when(repository.findByOwningService_NameOrderByCreationDateDesc("backup-service")).thenReturn(tasks);

		assertThat(service().listAll("backup-service")).isEqualTo(tasks);
	}

	/** What the ux_tasks_service_id_name_ci index raises: SQL state 23505, unique violation. */
	private DataIntegrityViolationException uniqueViolation() {
		return new DataIntegrityViolationException("could not execute statement", new SQLException(
				"duplicate key value violates unique constraint \"ux_tasks_service_id_name_ci\"", "23505"));
	}

}
