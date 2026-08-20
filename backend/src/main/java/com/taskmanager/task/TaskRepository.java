package com.taskmanager.task;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	boolean existsByNameIgnoreCaseAndOwningService_Id(String name, UUID serviceId);

	Optional<Task> findByNameIgnoreCaseAndOwningService_Id(String name, UUID serviceId);

	/**
	 * The owning service is fetched in the same query. Hibernate join-fetches an
	 * eager {@code @ManyToOne} only when loading by id; for a query like this one
	 * it would otherwise issue one extra select per distinct owner, and every
	 * {@link Task} rendered by the API needs its service name
	 * (api/openapi.yaml, {@code Task.service}).
	 */
	@EntityGraph(attributePaths = "owningService")
	List<Task> findAllByOrderByCreationDateDesc();

	@EntityGraph(attributePaths = "owningService")
	List<Task> findByOwningService_NameOrderByCreationDateDesc(String serviceName);

}
