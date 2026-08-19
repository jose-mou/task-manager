package com.taskmanager.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	boolean existsByNameIgnoreCase(String name);

	Optional<Task> findByNameIgnoreCase(String name);

	List<Task> findAllByOrderByCreationDateDesc();

}
