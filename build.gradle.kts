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

	// Test support: JUnit 5, Mockito, AssertJ, MockMvc, plus the per-starter test helpers.
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
