#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ "${1:-}" == "--docker" ]]; then
  if [[ ! -f "$ROOT_DIR/.env" && -f "$ROOT_DIR/.env.example" ]]; then
    cp "$ROOT_DIR/.env.example" "$ROOT_DIR/.env"
  fi
  exec docker compose -f "$ROOT_DIR/docker-compose.yml" up --build
fi

PIDS=()

cleanup() {
  local code=$?
  for pid in "${PIDS[@]:-}"; do
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done
  exit "$code"
}
trap cleanup INT TERM EXIT

if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
  echo "[Zenius One] Installing frontend dependencies..."
  npm --prefix "$ROOT_DIR/frontend" install
fi

mkdir -p "$ROOT_DIR/.zenius-data"

echo "[Zenius One] Building multi-module Microservices & Spring Cloud Gateway..."
mvn -f "$ROOT_DIR/services/pom.xml" -q package -DskipTests

IDENTITY_JAR="$(ls -1 "$ROOT_DIR"/services/identity-org-service/target/zenius-identity-org-service-*.jar | head -n 1)"
CUSTOMER_JAR="$(ls -1 "$ROOT_DIR"/services/customer-service/target/zenius-customer-service-*.jar | head -n 1)"
ERP_JAR="$(ls -1 "$ROOT_DIR"/services/erp-people-service/target/zenius-erp-people-service-*.jar | head -n 1)"
WORKPLACE_JAR="$(ls -1 "$ROOT_DIR"/services/workplace-ai-service/target/zenius-workplace-ai-service-*.jar | head -n 1)"
GATEWAY_JAR="$(ls -1 "$ROOT_DIR"/services/gateway-service/target/zenius-gateway-service-*.jar | head -n 1)"
DATA_DIR="$ROOT_DIR/.zenius-data"

wait_http() {
  local name=$1
  local url=$2
  for _ in $(seq 1 90); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "[Zenius One] $name is UP ($url)"
      return 0
    fi
    sleep 1
  done
  echo "[Zenius One] Timed out waiting for $name at $url"
  return 1
}

echo "[Zenius One] Starting identity-org-service (:8081, Flyway + 1,040 seeded records)..."
ZENIUS_DATA_DIR="$DATA_DIR" java -jar "$IDENTITY_JAR" --spring.profiles.active=local &
PIDS+=($!)
wait_http "identity-org-service" "http://localhost:8081/api/v1/system/status"

echo "[Zenius One] Starting domain microservices: customer (:8082), erp-people (:8083), workplace-ai (:8084)..."
ZENIUS_DATA_DIR="$DATA_DIR" java -jar "$CUSTOMER_JAR" --spring.profiles.active=local &
PIDS+=($!)
ZENIUS_DATA_DIR="$DATA_DIR" java -jar "$ERP_JAR" --spring.profiles.active=local &
PIDS+=($!)
ZENIUS_DATA_DIR="$DATA_DIR" java -jar "$WORKPLACE_JAR" --spring.profiles.active=local &
PIDS+=($!)

wait_http "customer-service" "http://localhost:8082/actuator/health"
wait_http "erp-people-service" "http://localhost:8083/actuator/health"
wait_http "workplace-ai-service" "http://localhost:8084/actuator/health"

echo "[Zenius One] Starting Spring Cloud gateway-service (:8080)..."
java -jar "$GATEWAY_JAR" &
PIDS+=($!)
wait_http "gateway-service" "http://localhost:8080/api/v1/system/microservices"

echo "[Zenius One] Starting React 19 + Vite UI (:5173)..."
echo "[Zenius One] App Ready -> http://localhost:5173 | Sign in: admin@zeniusone.local / Zenius@2026!"
npm --prefix "$ROOT_DIR/frontend" run dev &
PIDS+=($!)

wait "${PIDS[@]}"
