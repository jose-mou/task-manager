package com.taskmanager.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	boolean existsByNameIgnoreCaseAndOwningService_Id(String name, UUID serviceId);

	Optional<Task> findByNameIgnoreCaseAndOwningService_Id(String name, UUID serviceId);

	List<Task> findAllByOrderByCreationDateDesc();

	List<Task> findByOwningService_NameOrderByCreationDateDesc(String serviceName);

}
