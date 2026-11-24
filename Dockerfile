# Packs the Login Service into one runnable image.
# Build:  docker build -t zodiac-login-service .
# Run:    see docs/DEPLOYMENT.md (it needs database settings as environment variables)
#
# Two stages ("multi-stage build"): the first has the full Java toolkit and compiles the code;
# the second is a small image that contains only the finished jar. The compiler and the source
# code never ship, which keeps the final image small.

# ---------- Stage 1: build the jar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy ONLY the build files first and download dependencies. Docker caches each step, so while
# build.gradle.kts is unchanged this slow download is skipped on later builds.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

# Now the code, which changes often. Tests are not run here: CI runs them before this image is built.
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---------- Stage 2: the image that actually runs ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Do not run as root: if the app were ever compromised, it would have far fewer rights.
RUN addgroup -S zodiac && adduser -S zodiac -G zodiac
USER zodiac

COPY --from=build --chown=zodiac:zodiac /workspace/build/libs/*.jar app.jar

EXPOSE 8081

# The platform (or Docker) asks "are you alive?" through the health endpoint.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

# Use at most 75% of the container's memory for Java's heap, so the container is not killed for
# using too much. Database settings and the profile come from environment variables at run time.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
