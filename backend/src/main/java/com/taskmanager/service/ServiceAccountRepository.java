package com.taskmanager.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceAccountRepository extends JpaRepository<ServiceAccount, UUID> {

	Optional<ServiceAccount> findByApiKey(String apiKey);

	Optional<ServiceAccount> findByName(String name);

	List<ServiceAccount> findAllByOrderByNameAsc();

}
