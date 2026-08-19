package com.taskmanager.task.web;

import com.taskmanager.task.DuplicateTaskNameException;
import com.taskmanager.task.Task;
import com.taskmanager.task.TaskFields;
import com.taskmanager.task.TaskNotFoundException;
import com.taskmanager.task.TaskService;
import com.taskmanager.task.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TaskService taskService;

	private Task sampleTask(String name) {
		return Task.create(name, "backup-service", "desc", TaskStatus.CREATED, "/opt/scripts/backup.sh", null, null,
				false, Instant.parse("2026-08-19T10:15:00Z"));
	}

	@Test
	void createReturns201WithTaskBody() throws Exception {
		Task created = sampleTask("Nightly backup");
		when(taskService.create(any(TaskFields.class))).thenReturn(created);

		mockMvc
			.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Nightly backup","service":"backup-service","script":"/opt/scripts/backup.sh"}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(created.getId().toString()))
			.andExpect(jsonPath("$.status").value("CREATED"))
			.andExpect(jsonPath("$.scheduled").value(false))
			.andExpect(jsonPath("$.creationDate").value("2026-08-19T10:15:00Z"))
			.andExpect(jsonPath("$.modificationDate").value("2026-08-19T10:15:00Z"));
	}

	@Test
	void createReturns400WithFieldErrorsWhenValidationFails() throws Exception {
		mockMvc
			.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"   ","service":"svc","script":"s","maxExecutions":0}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("name"))
			.andExpect(jsonPath("$.errors[1].field").value("maxExecutions"));
	}

	@Test
	void createReturns409WhenNameIsDuplicate() throws Exception {
		when(taskService.create(any(TaskFields.class))).thenThrow(new DuplicateTaskNameException("backup"));

		mockMvc
			.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"backup","service":"svc","script":"s"}
						"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("A task with name 'backup' already exists"));
	}

	@Test
	void updateReturns200WithUpdatedTask() throws Exception {
		Task updated = sampleTask("Nightly backup");
		UUID id = updated.getId();
		when(taskService.update(eq(id), any(TaskFields.class))).thenReturn(updated);

		mockMvc
			.perform(put("/api/tasks/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Nightly backup","service":"backup-service","script":"/opt/scripts/backup.sh","status":"COMPLETED"}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id.toString()));
	}

	@Test
	void updateReturns404WhenTaskDoesNotExist() throws Exception {
		UUID id = UUID.randomUUID();
		when(taskService.update(eq(id), any(TaskFields.class))).thenThrow(new TaskNotFoundException(id));

		mockMvc
			.perform(put("/api/tasks/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Ghost","service":"svc","script":"s"}
						"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Task " + id + " not found"));
	}

	@Test
	void listReturnsTasksOrderedByServiceCall() throws Exception {
		Task first = sampleTask("A");
		Task second = sampleTask("B");
		when(taskService.listAll()).thenReturn(List.of(first, second));

		mockMvc.perform(get("/api/tasks"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(first.getId().toString()))
			.andExpect(jsonPath("$[1].id").value(second.getId().toString()));
	}

	@Test
	void getReturnsTaskById() throws Exception {
		Task task = sampleTask("A");
		when(taskService.getById(task.getId())).thenReturn(task);

		mockMvc.perform(get("/api/tasks/" + task.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(task.getId().toString()));
	}

	@Test
	void getReturns404WhenTaskDoesNotExist() throws Exception {
		UUID id = UUID.randomUUID();
		when(taskService.getById(id)).thenThrow(new TaskNotFoundException(id));

		mockMvc.perform(get("/api/tasks/" + id))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Task " + id + " not found"));
	}

	@Test
	void createReturns400InTheContractShapeWhenAFieldHasTheWrongJsonType() throws Exception {
		mockMvc
			.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Nightly backup","service":"svc","script":"s","maxExecutions":"many"}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("maxExecutions"));
	}

	@Test
	void createReturns400InTheContractShapeWhenTheBodyIsNotJson() throws Exception {
		mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("not json at all"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("body"));
	}

	@Test
	void getReturns404WhenTheIdIsNotAUuid() throws Exception {
		mockMvc.perform(get("/api/tasks/not-a-uuid"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Task not-a-uuid not found"));
	}

}
