#!/usr/bin/env bash
# Starts everything needed to use the Login Service on your laptop, with one command:
#   1. the PostgreSQL database (Docker)
#   2. the web pages on http://localhost:5500
#   3. the backend on http://localhost:8081
# Stop with Ctrl+C. (The database keeps running; stop it with: docker compose down)
#
# Run from anywhere:  ./scripts/dev.sh

set -euo pipefail                 # stop at the first error, and on unset variables
cd "$(dirname "$0")/.."           # always work from the project folder

fail() { echo "✗ $1" >&2; exit 1; }

# --- 1. Check the things that most often go wrong, with a clear message for each ---
command -v docker >/dev/null || fail "Docker is not installed. Install Docker Desktop first."
docker info >/dev/null 2>&1   || fail "Docker is not running. Open Docker Desktop, wait for the whale icon, then try again."
command -v python3 >/dev/null || fail "python3 is needed to serve the web pages."
command -v java >/dev/null    || fail "Java 21 is needed. Install a JDK 21."

for port in 8081 5500; do
  if lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    fail "Port $port is already in use. Is the service already running in another terminal?"
  fi
done

# --- 2. Database. --wait returns only once the database reports healthy. ---
echo "▶ Starting the database…"
docker compose up -d --wait

# --- 3. Web pages in the background; make sure they stop when this script ends. ---
python3 -m http.server 5500 --directory frontend >/dev/null 2>&1 &
FRONTEND_PID=$!
trap 'kill "$FRONTEND_PID" 2>/dev/null || true' EXIT

cat <<EOF

  Web pages   http://localhost:5500
  API docs    http://localhost:8081/swagger-ui.html
  Health      http://localhost:8081/actuator/health

▶ Starting the backend (first run downloads dependencies; this takes a minute)…
EOF

# --- 4. Backend in the foreground, so its log is on screen and Ctrl+C stops it. ---
./gradlew bootRun
