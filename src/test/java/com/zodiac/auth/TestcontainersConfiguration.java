package com.zodiac.auth;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a real, throwaway PostgreSQL in Docker for tests.
 *
 * <p>{@code @ServiceConnection} tells Spring Boot to connect to this container automatically,
 * so tests need no DB_URL / DB_USERNAME / DB_PASSWORD.
 *
 * <p>The image version matches docker-compose.yml, so tests run against the same database as local dev.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17.12-alpine"));
	}

}
