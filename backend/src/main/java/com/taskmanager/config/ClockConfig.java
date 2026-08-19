package com.taskmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/** Provides the application-wide {@link Clock}, fixed to UTC as required by the API contract. */
@Configuration
public class ClockConfig {

	/**
	 * Ticks in microseconds, the precision of a PostgreSQL {@code timestamptz}: a
	 * finer clock would make the timestamps returned right after a write differ
	 * from the ones read back from the database on the next request.
	 */
	@Bean
	public Clock clock() {
		return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
	}

}
