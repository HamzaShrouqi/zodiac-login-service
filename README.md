# Zodiac Login Service

Authentication service for the Zodiac boutique platform (perfumes, cosmetics, skincare, accessories, gifts).
It owns user credentials, roles and login sessions. Customer profile data lives in the future Onboarding Service.

## Tech stack

Java 21 · Spring Boot 4.2 · Gradle (Kotlin DSL) · PostgreSQL · Liquibase

## Prerequisites

- JDK 21
- Nothing else for now. The Gradle Wrapper (`./gradlew`) downloads the right Gradle version automatically.

## Run locally

```bash
./gradlew bootRun
```

The service starts on <http://localhost:8081>. Health check:

```bash
curl http://localhost:8081/actuator/health
```

## Test

```bash
./gradlew test
```

## Build a runnable jar

```bash
./gradlew bootJar
java -jar build/libs/login-service-0.0.1-SNAPSHOT.jar
```
