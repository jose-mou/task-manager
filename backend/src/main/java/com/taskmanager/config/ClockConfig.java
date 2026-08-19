package com.taskmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Provides the application-wide {@link Clock}, fixed to UTC as required by the API contract. */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
