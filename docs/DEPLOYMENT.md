# Deployment notes

How the Login Service is built, packaged and configured so it can run in any environment.
Nothing is deployed anywhere yet; this page describes what is ready and what remains to be chosen.

## Environments

| | Local | Review | Production |
|---|---|---|---|
| Purpose | Developing on a laptop | A temporary copy of a change, to look at it before merging | The real service |
| Status | Ready | Planned | Planned |
| Database | Docker Compose PostgreSQL | Its own throwaway PostgreSQL | Managed PostgreSQL (Amazon Aurora PostgreSQL is the intended target) |
| How it starts | `./scripts/dev.sh` | Container from the CI-built image | Container from the CI-built image |
| Settings come from | `application-local.yml` | Environment variables | Environment variables |
| API docs page | On | Optional | Off |

The same image runs in every environment. Only the environment variables change.

## The container image

Built by the [Dockerfile](../Dockerfile) in two stages: a Java 21 JDK compiles the code, then only the jar is copied into a small Java 21 JRE image that runs as a non-root user (about 417 MB).

```bash
docker build -t zodiac-login-service .
```

Run it (here against the local Compose database; `host.docker.internal` means "the machine running Docker" on Docker Desktop):

```bash
docker run --rm -p 8081:8081 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/login_db \
  -e DB_USERNAME=login_user \
  -e DB_PASSWORD=login_local_pw \
  zodiac-login-service
```

### Settings the container needs

| Variable | Required | Meaning |
|---|---|---|
| `DB_URL` | yes | JDBC URL of the PostgreSQL database |
| `DB_USERNAME` | yes | Database user |
| `DB_PASSWORD` | yes | Database password. Comes from the platform's secret store, never from Git or the image |
| `ZODIAC_CORS_ALLOWED_ORIGINS` | if a website calls the API | Comma-separated web origins, e.g. `https://shop.example.com` |
| `SPRING_PROFILES_ACTIVE` | no | Leave unset outside local development |
| `SERVER_PORT` | no | Default 8081 |

If `DB_URL` is missing the container exits immediately with `'url' must start with "jdbc"`.

### Health checks

| URL | Meaning | Use it for |
|---|---|---|
| `/actuator/health/liveness` | The process is running | "Restart me if this fails" |
| `/actuator/health/readiness` | Ready to receive traffic | "Send me requests only when this is UP" |
| `/actuator/health` | Overall status | Docker's built-in `HEALTHCHECK` |

## Database migrations

Liquibase runs automatically when the service starts and applies only the changelogs not yet recorded in `databasechangelog`. It takes a lock first, so if two copies start at once only one migrates.

Rules that keep deployments safe:
- Never edit a changelog that has already run anywhere. Add a new file.
- While old and new versions run side by side, a migration must work for both. Add a column first, switch the code to use it in a later release, and drop the old column in a release after that.

## Continuous integration (GitHub Actions)

[.github/workflows/ci.yml](../.github/workflows/ci.yml) runs on every push to `main` and on every pull request:

1. **Build and test**: Java 21, `./gradlew build` (all unit and integration tests, with Testcontainers PostgreSQL). On failure the HTML test report is saved on the run page.
2. **Build Docker image**: runs only after step 1 passes, and proves the Dockerfile still works. Nothing is published.

A new push to the same branch cancels the older run. Settings to turn on in the GitHub repository (Settings → Branches): require the `Build and test` check to pass before merging into `main`.

## Still to decide before a real deployment

| Decision | Options to compare |
|---|---|
| Where the container runs | A managed container service on AWS, or another host |
| Where images are stored | A container registry matching the host |
| Where secrets live | The host's secret store |
| Image tags | The Git commit id, so every running version can be traced to its code |
| How a change reaches production | CI builds and pushes the image, deploys to Review, runs a health check, then promotes to Production |

Each of these gets its own comparison and your approval when we reach it.
