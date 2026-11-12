#!/usr/bin/env bash
# run-backend.sh — start the YEP Spring Boot backend locally
# Usage: ./run-backend.sh [demo]
# Pass "demo" to seed demo accounts: ./run-backend.sh demo

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/backend"

PROFILE="${1:-dev}"

echo "▶ Starting YEP backend (profile: $PROFILE) ..."
echo "  Using DB: ${DB_URL:-jdbc:postgresql://localhost:5432/yep}"

if [ -f mvnw ]; then
  ./mvnw spring-boot:run -Dspring-boot.run.profiles="$PROFILE"
elif command -v mvn &>/dev/null; then
  mvn spring-boot:run -Dspring-boot.run.profiles="$PROFILE"
else
  JAR=$(ls target/yep-backend-*.jar 2>/dev/null | head -1)
  if [ -z "$JAR" ]; then
    echo "ERROR: No mvnw, no mvn, and no built JAR found. Run 'mvn package' first." >&2
    exit 1
  fi
  echo "  Running pre-built JAR: $JAR"
  java -Dspring.profiles.active="$PROFILE" -jar "$JAR"
fi
