plugins {
	java
	// Adds bootRun / bootJar tasks and packages the app as one runnable "fat" jar.
	id("org.springframework.boot") version "4.2.0"
	// Lets us omit versions on Spring dependencies: the Spring Boot BOM picks
	// versions that are tested to work together.
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.zodiac"
version = "0.0.1-SNAPSHOT"
description = "Zodiac Login Service"

java {
	toolchain {
		// Compile and run with Java 21, no matter which JDK is the machine's default.
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// REST controllers + embedded Tomcat server.
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	// Bean Validation (@NotBlank, @Email...) for request DTOs.
	implementation("org.springframework.boot:spring-boot-starter-validation")
	// Health endpoint (/actuator/health) used by deployment platforms.
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	// JPA + Hibernate: lets Java classes be saved to / loaded from database tables.
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// Liquibase: creates and changes tables from versioned changelog files at startup.
	implementation("org.springframework.boot:spring-boot-starter-liquibase")
	// PostgreSQL driver: only needed while the app runs, never in our code directly.
	runtimeOnly("org.postgresql:postgresql")
	// Only the password-hashing part of Spring Security (BCrypt), not the whole framework.
	implementation("org.springframework.security:spring-security-crypto")
	// Generates the OpenAPI description and the Swagger UI page from our controllers.
	// Not part of Spring Boot's tested-together list, so the version is pinned here.
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.1")

	// Test support: JUnit 5, Mockito, AssertJ, MockMvc, plus the per-starter test helpers.
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
	// Testcontainers: tests start a real, throwaway PostgreSQL in Docker.
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// `./gradlew bootRun` is only used on a developer laptop, so it always uses the "local" profile.
// A real deployment sets SPRING_PROFILES_ACTIVE itself.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	systemProperty("spring.profiles.active", "local")
}
