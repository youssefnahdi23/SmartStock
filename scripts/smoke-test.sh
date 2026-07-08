#!/usr/bin/env bash
#
# smoke-test.sh — SmartStock AI full-stack runtime verification (M-2).
#
# Verifies that every service in the Docker Compose stack starts cleanly and
# its /actuator/health endpoint returns {"status":"UP"}.
#
# Usage:
#   mvn -f services/pom.xml clean package -DskipTests
#   docker compose up -d --build
#   bash scripts/smoke-test.sh
#
# Options (env vars):
#   HOST            Target hostname (default: localhost)
#   TIMEOUT         Max seconds to wait per service (default: 300)
#   RUN_HAPPY_PATH  Set to 1 to run the business-flow Phase 2 (requires seed data)
#   SMOKE_USER      Username for Phase 2 auth (default: admin)
#   SMOKE_PASSWORD  Password for Phase 2 auth (required if RUN_HAPPY_PATH=1)
#   NO_COLOR        Set to 1 to disable ANSI color output

set -uo pipefail

# ─── Color helpers ────────────────────────────────────────────────────────────
if [ "${NO_COLOR:-0}" = "1" ] || ! [ -t 1 ]; then
  GREEN=""; YELLOW=""; RED=""; CYAN=""; BOLD=""; RESET=""
else
  GREEN="\033[0;32m"; YELLOW="\033[0;33m"; RED="\033[0;31m"
  CYAN="\033[0;36m"; BOLD="\033[1m"; RESET="\033[0m"
fi

ok()   { echo -e "  ${GREEN}✓${RESET} $*"; }
warn() { echo -e "  ${YELLOW}!${RESET} $*"; }
fail() { echo -e "  ${RED}✗${RESET} $*" >&2; }
info() { echo -e "  ${CYAN}→${RESET} $*"; }

HOST="${HOST:-localhost}"
TIMEOUT="${TIMEOUT:-300}"
PASS=0
FAIL=0

# ─── Infrastructure endpoints ─────────────────────────────────────────────────
# name  port  path
INFRA=(
  "kafka-ui-check        9092 /"          # just a TCP check via curl -s (will fail HTTP but proves port open)
  "prometheus            9090 /-/healthy"
  "grafana               3000 /api/health"
  "loki                  3100 /ready"
  "tempo                 3200 /status"
  "mailpit               8025 /api/v1/info"
)

# ─── Application service endpoints ───────────────────────────────────────────
SERVICES=(
  "api-gateway           8080 /actuator/health"
  "identity-service      8001 /api/v1/actuator/health"
  "product-service       8002 /api/v1/actuator/health"
  "inventory-service     8003 /api/v1/actuator/health"
  "warehouse-service     8004 /api/v1/actuator/health"
  "supplier-service      8005 /api/v1/actuator/health"
  "customer-service      8006 /api/v1/actuator/health"
  "purchase-order-service 8007 /api/v1/actuator/health"
  "sales-order-service   8008 /api/v1/actuator/health"
)

# ─── Helpers ──────────────────────────────────────────────────────────────────
check_infra_up() {
  local name="$1" port="$2" path="$3"
  local url="http://${HOST}:${port}${path}"
  if curl -fs --max-time 5 "$url" >/dev/null 2>&1; then
    ok "${name} (${url})"
    PASS=$(( PASS + 1 ))
    return 0
  else
    warn "${name} (${url}) — not responding (non-critical)"
    return 1
  fi
}

wait_for_up() {
  local name="$1" port="$2" path="$3"
  local url="http://${HOST}:${port}${path}"
  local deadline=$(( $(date +%s) + TIMEOUT ))
  local elapsed=0
  echo -ne "  ${CYAN}→${RESET} ${BOLD}${name}${RESET} (${url}) "
  while [ "$(date +%s)" -lt "$deadline" ]; do
    if curl -fs --max-time 5 "$url" 2>/dev/null | grep -q '"status":"UP"'; then
      echo -e "... ${GREEN}UP${RESET}"
      PASS=$(( PASS + 1 ))
      return 0
    fi
    echo -n "."
    sleep 5
  done
  echo -e " ${RED}TIMEOUT${RESET} after ${TIMEOUT}s"
  FAIL=$(( FAIL + 1 ))
  return 1
}

check_tcp() {
  local name="$1" host="$2" port="$3"
  if curl -fs --max-time 3 "telnet://${host}:${port}" >/dev/null 2>&1 \
     || nc -z -w3 "${host}" "${port}" 2>/dev/null; then
    ok "${name} listening on ${host}:${port}"
    PASS=$(( PASS + 1 ))
    return 0
  else
    warn "${name} port ${port} — not reachable from host"
    return 1
  fi
}

# ═════════════════════════════════════════════════════════════════════════════
# PHASE 0: Infrastructure TCP / HTTP spot-checks (non-blocking — warn only)
# ═════════════════════════════════════════════════════════════════════════════
echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════════════╗${RESET}"
echo -e "${BOLD}║   SmartStock AI — Smoke Test (M-2)                  ║${RESET}"
echo -e "${BOLD}╚══════════════════════════════════════════════════════╝${RESET}"
echo -e "  Host: ${HOST}   Timeout: ${TIMEOUT}s   $(date '+%Y-%m-%d %H:%M:%S')"
echo ""
echo -e "${BOLD}── Phase 0: Infrastructure spot-checks ─────────────────────${RESET}"

check_tcp "PostgreSQL (identity)"     "${HOST}" 5432 || true
check_tcp "PostgreSQL (product)"      "${HOST}" 5433 || true
check_tcp "PostgreSQL (inventory)"    "${HOST}" 5434 || true
check_tcp "PostgreSQL (warehouse)"    "${HOST}" 5435 || true
check_tcp "PostgreSQL (supplier)"     "${HOST}" 5436 || true
check_tcp "PostgreSQL (customer)"     "${HOST}" 5437 || true
check_tcp "PostgreSQL (purchase-ord)" "${HOST}" 5438 || true
check_tcp "PostgreSQL (sales-ord)"    "${HOST}" 5439 || true
check_tcp "Redis"                     "${HOST}" 6379  || true
check_tcp "Kafka"                     "${HOST}" 9092  || true
check_tcp "Zookeeper"                 "${HOST}" 2181  || true

echo ""
echo -e "${BOLD}── Phase 0b: Observability HTTP checks ─────────────────────${RESET}"
check_infra_up "prometheus" 9090 "/-/healthy"   || true
check_infra_up "grafana"    3000 "/api/health"  || true
check_infra_up "loki"       3100 "/ready"       || true
check_infra_up "tempo"      3200 "/status"      || true
check_infra_up "mailpit"    8025 "/api/v1/info" || true

# ═════════════════════════════════════════════════════════════════════════════
# PHASE 1: Application service readiness (deterministic — fail on timeout)
# ═════════════════════════════════════════════════════════════════════════════
echo ""
echo -e "${BOLD}── Phase 1: Service readiness (poll until UP or ${TIMEOUT}s) ────${RESET}"
rc=0
for entry in "${SERVICES[@]}"; do
  # shellcheck disable=SC2086
  set -- $entry
  wait_for_up "$1" "$2" "$3" || rc=1
done

echo ""
echo -e "${BOLD}── Phase 1 summary ──────────────────────────────────────────${RESET}"
if [ "$rc" -ne 0 ]; then
  echo -e "  ${RED}${BOLD}FAIL${RESET} — one or more services did not reach status UP within ${TIMEOUT}s"
  echo -e "  ${YELLOW}Tip:${RESET} docker compose logs <service> | tail -50"
  echo ""
  exit 1
fi
echo -e "  ${GREEN}${BOLD}PASS${RESET} — all ${#SERVICES[@]} application services are UP"

# ═════════════════════════════════════════════════════════════════════════════
# PHASE 2: Happy path (optional — enable with RUN_HAPPY_PATH=1)
# ═════════════════════════════════════════════════════════════════════════════
if [ "${RUN_HAPPY_PATH:-0}" != "1" ]; then
  echo ""
  echo -e "  ${YELLOW}Phase 2 skipped${RESET} — set RUN_HAPPY_PATH=1 to run the business flow."
  echo ""
  echo -e "${GREEN}${BOLD}SMOKE OK${RESET} (readiness)"
  exit 0
fi

echo ""
echo -e "${BOLD}── Phase 2: Happy path ─────────────────────────────────────${RESET}"
echo -e "  Flow: authenticate → create product → stock-in → sales order → deliver"

# Login goes through the gateway (8080): it rewrites /api/v1/auth/** onto
# identity's internal /api/v1/identity/auth/** layout, like any real client.
TOKEN="$(curl -fs -X POST "http://${HOST}:8080/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${SMOKE_USER:-admin}\",\"password\":\"${SMOKE_PASSWORD:?set SMOKE_PASSWORD to run Phase 2}\"}" \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"

if [ -z "$TOKEN" ]; then
  fail "Could not obtain auth token from identity-service"
  exit 1
fi
ok "Auth token obtained"

AUTH_HEADER="Authorization: Bearer ${TOKEN}"
info "Request bodies are environment-specific — see docs/deployment/full-stack-compose.md"
echo ""
echo -e "${GREEN}${BOLD}SMOKE OK${RESET} (readiness + happy-path scaffold)"
