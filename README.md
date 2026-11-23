# Zodiac Login Service

Authentication service for the Zodiac boutique platform (perfumes, cosmetics, skincare, accessories, gifts).
It owns user credentials, roles and login sessions. Customer profile data lives in the future Onboarding Service.

## Tech stack

Java 21 · Spring Boot 4.2 · Gradle (Kotlin DSL) · PostgreSQL · Liquibase

## Prerequisites

- JDK 21
- Docker Desktop (running). It provides PostgreSQL locally and in tests.
- No Gradle install needed. The Gradle Wrapper (`./gradlew`) downloads the right version automatically.

## Run locally

Start the database (first time, or after a reboot):

```bash
docker compose up -d
```

Start the service (uses the `local` profile automatically):

```bash
./gradlew bootRun
```

On startup, Liquibase creates or updates the tables from `src/main/resources/db/changelog/`.

Look inside the database:

```bash
docker exec -it zodiac-login-db psql -U login_user -d login_db
```

Inside `psql`: `\dt` lists tables, `\d user_account` describes one, `\q` quits.

Stop the database with `docker compose down`. Wipe all local data with `docker compose down -v`.

## Configuration

| Variable | Meaning |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://host:5432/login_db` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials |
| `SPRING_PROFILES_ACTIVE` | Environment profile (`local` for laptops) |
| `SERVER_PORT` | HTTP port (default 8081) |

## Database schema

- `user_account`: one row per person who can log in (customer, staff, admin)
- `auth_session`: one row per login session (only a hash of the token is stored)

Schema changes are made only by adding new Liquibase changelog files. Never edit one that has already run.

The service starts on <http://localhost:8081>. Health check:

```bash
curl http://localhost:8081/actuator/health
```

## Test

```bash
./gradlew test
```

Tests need Docker running. Testcontainers starts a fresh, throwaway PostgreSQL for each run.

## Build a runnable jar

```bash
./gradlew bootJar
SPRING_PROFILES_ACTIVE=local java -jar build/libs/login-service-0.0.1-SNAPSHOT.jar
```
