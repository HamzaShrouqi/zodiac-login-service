# Zodiac Login Service

[![CI](https://github.com/HamzaShrouqi/zodiac-login-service/actions/workflows/ci.yml/badge.svg)](https://github.com/HamzaShrouqi/zodiac-login-service/actions/workflows/ci.yml)

Accounts and login for the Zodiac boutique platform (perfumes, cosmetics, skincare, accessories, gifts).
It owns credentials, roles and login sessions. Customer profile data will live in the future Onboarding Service.

**Stack:** Java 21 · Spring Boot 4.2 · Gradle (Kotlin DSL) · PostgreSQL 17 · Liquibase · plain HTML/CSS/JS frontend

## Quick start

You need **JDK 21**, **Docker Desktop** (running) and **python3**. Nothing else: Gradle downloads itself.

```bash
git clone git@github.com:HamzaShrouqi/zodiac-login-service.git
cd zodiac-login-service
./scripts/dev.sh
```

That starts the database, the web pages and the backend. When the log says `Started LoginServiceApplication`:

| What | Where |
|---|---|
| Web pages (register, log in, account) | <http://localhost:5500> |
| API documentation (try every endpoint) | <http://localhost:8081/swagger-ui.html> |
| Health check | <http://localhost:8081/actuator/health> |

Stop with `Ctrl+C`. Stop the database with `docker compose down` (`docker compose down -v` also wipes its data).

## Documentation

- [Architecture overview](docs/ARCHITECTURE.md): diagrams, data model, decisions
- [Deployment notes](docs/DEPLOYMENT.md): environments, container image, health checks, CI
- [Frontend notes](frontend/README.md)
- API reference: the Swagger page above. The raw OpenAPI file is at <http://localhost:8081/v3/api-docs>.

## API at a glance

Base path `/api/v1/auth`. Protected endpoints need `Authorization: Bearer <accessToken>`.

| Method | Path | Auth | Success | Purpose |
|---|---|---|---|---|
| POST | `/register` | – | 201 | Create a customer account |
| POST | `/login` | – | 200 | Log in, receive a token |
| POST | `/logout` | ✓ | 204 | End this session |
| GET | `/me` | ✓ | 200 | Who is logged in |
| PUT | `/password` | ✓ | 204 | Change password; other sessions end |

Every error has the same shape: `{code, message, fieldErrors, timestamp}`.

| HTTP | `code` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Bad input (see `fieldErrors`) |
| 400 | `MALFORMED_REQUEST` | Body isn't valid JSON |
| 400 | `INCORRECT_CURRENT_PASSWORD` | Wrong current password on change |
| 400 | `PASSWORD_TOO_LONG` | Password over 72 bytes |
| 401 | `INVALID_CREDENTIALS` | Wrong email or password |
| 401 | `INVALID_SESSION` | Missing, invalid, expired or logged-out token |
| 403 | `ACCOUNT_DISABLED` | Account disabled by an admin |
| 409 | `EMAIL_ALREADY_REGISTERED` | Email taken |
| 423 | `ACCOUNT_LOCKED` | Too many wrong passwords (temporary) |

To call every endpoint from the command line, including the error cases (service must be running):

```bash
./scripts/try-endpoints.sh
```

## Running things separately

```bash
docker compose up -d                                    # database only
./gradlew bootRun                                       # backend (uses the "local" profile)
python3 -m http.server 5500 --directory frontend        # web pages
```

Look inside the database:

```bash
docker exec -it zodiac-login-db psql -U login_user -d login_db
```

Inside `psql`: `\dt` lists tables, `\d user_account` describes one, `\q` quits.

## Tests

```bash
./gradlew test
```

Docker must be running: Testcontainers starts a fresh, throwaway PostgreSQL for each run.

| Kind | Where | Uses | Speed |
|---|---|---|---|
| Unit | `src/test/.../service`, `security`, `entity` | JUnit 5 + Mockito, no Spring, no database | milliseconds |
| Integration | `src/test/.../integration` | Whole app + real PostgreSQL, MockMvc | seconds |

`MutableClock` lets tests move time forward (expired sessions, lock timeouts) without waiting.
Run one class: `./gradlew test --tests '*AuthServiceTest'`. HTML report: `build/reports/tests/test/index.html`.

## Build a runnable jar

```bash
./gradlew bootJar
```

```bash
DB_URL=jdbc:postgresql://localhost:5432/login_db DB_USERNAME=login_user DB_PASSWORD=login_local_pw \
  java -jar build/libs/login-service-0.0.1-SNAPSHOT.jar
```

## Run as a container

```bash
docker build -t zodiac-login-service .
```

```bash
docker run --rm -p 8081:8081 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/login_db \
  -e DB_USERNAME=login_user -e DB_PASSWORD=login_local_pw \
  zodiac-login-service
```

Needs the database running (`docker compose up -d`). More in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Configuration

Settings come from environment variables, so the same jar runs everywhere.

| Variable | Meaning |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://host:5432/login_db` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials |
| `SPRING_PROFILES_ACTIVE` | Environment profile (`local` for laptops) |
| `SERVER_PORT` | HTTP port (default 8081) |
| `ZODIAC_CORS_ALLOWED_ORIGINS` | Websites allowed to call the API from a browser, comma-separated (default: none) |

The login rules (`zodiac.auth.*` in `application.yml`): session lifetime 7 days, 5 failed attempts before a 15-minute lock.

## Database

- `user_account`: one row per person who can log in (customer, staff, admin)
- `auth_session`: one row per login session (only a hash of the token is stored)

Schema changes are made only by adding new Liquibase changelog files in `src/main/resources/db/changelog/`. Never edit one that has already run.

## Project structure

```
login-service/
├── build.gradle.kts, settings.gradle.kts, gradlew      build setup (the wrapper pins Gradle)
├── docker-compose.yml                                  local PostgreSQL
├── Dockerfile, .dockerignore                           container image
├── .github/workflows/ci.yml                            build + test on every push
├── scripts/                                            dev.sh (start everything), try-endpoints.sh
├── docs/ARCHITECTURE.md
├── frontend/                                           static pages
└── src/
    ├── main/java/com/zodiac/auth/
    │   ├── config/  controller/  service/  repository/
    │   └── entity/  dto/  security/  exception/
    ├── main/resources/
    │   ├── application.yml, application-local.yml
    │   └── db/changelog/                               Liquibase migrations
    └── test/java/com/zodiac/auth/                      unit + integration tests
```

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Docker is not running` or `Could not find a valid Docker environment` | Open Docker Desktop and wait for the whale icon |
| `Port 8081 / 5500 is already in use` | Stop the other copy of the service, or set `SERVER_PORT` |
| `port 5432 is already allocated` | Another PostgreSQL is using that port; stop it |
| Backend won't start: `'url' must start with "jdbc"` | The database settings are missing. Run through `./gradlew bootRun` (sets the profile) or set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| Web page says "Cannot reach the server" | Backend isn't running, or the page isn't served from `http://localhost:5500` |
