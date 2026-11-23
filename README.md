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

## API

Base path: `/api/v1/auth`. Protected endpoints need the header `Authorization: Bearer <accessToken>`.

| Method | Path | Auth | Success | Purpose |
|---|---|---|---|---|
| POST | `/register` | – | 201 + user | Create a customer account `{email, password}` |
| POST | `/login` | – | 200 + token | Log in `{email, password}` |
| POST | `/logout` | ✓ | 204 | End this session |
| GET | `/me` | ✓ | 200 + user | Who is logged in |
| PUT | `/password` | ✓ | 204 | Change password `{currentPassword, newPassword}` and log out other devices |

Every error has the same JSON shape: `{code, message, fieldErrors, timestamp}`.

| HTTP | `code` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Bad input (see `fieldErrors`) |
| 400 | `MALFORMED_REQUEST` | Body isn't valid JSON |
| 400 | `INCORRECT_CURRENT_PASSWORD` | Wrong current password on change |
| 401 | `INVALID_CREDENTIALS` | Wrong email or password |
| 401 | `INVALID_SESSION` | Missing, invalid, expired or logged-out token |
| 403 | `ACCOUNT_DISABLED` | Account disabled by an admin |
| 409 | `EMAIL_ALREADY_REGISTERED` | Email taken |
| 423 | `ACCOUNT_LOCKED` | Too many wrong passwords (temporary) |

Try every endpoint at once (service must be running):

```bash
./scripts/try-endpoints.sh
```

## Frontend

Simple HTML/CSS/JS pages (log in, register, my account) live in `frontend/`.
See [frontend/README.md](frontend/README.md). Short version, with the backend running:

```bash
python3 -m http.server 5500 --directory frontend
```

Then open <http://localhost:5500>.

## Configuration

| Variable | Meaning |
|---|---|
| `ZODIAC_CORS_ALLOWED_ORIGINS` | Websites allowed to call the API from a browser, comma-separated (default: none). The `local` profile allows `http://localhost:5500` |
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

| Kind | Where | Uses | Speed |
|---|---|---|---|
| Unit tests | `src/test/.../service`, `security`, `entity` | JUnit 5 + Mockito, no Spring, no database | milliseconds |
| Integration tests | `src/test/.../integration` | Whole app + real PostgreSQL, calls endpoints with MockMvc | seconds |

`MutableClock` lets tests move time forward (expired sessions, lock timeouts) without waiting.

Run one test class: `./gradlew test --tests '*AuthServiceTest'`. The HTML report is at `build/reports/tests/test/index.html`.

## Build a runnable jar

```bash
./gradlew bootJar
SPRING_PROFILES_ACTIVE=local java -jar build/libs/login-service-0.0.1-SNAPSHOT.jar
```
