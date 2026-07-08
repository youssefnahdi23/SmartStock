#!/usr/bin/env bash
#
# SmartStock Database Restore
# ---------------------------
# Restores a single service database from an S3/MinIO dump produced by the backup
# CronJob (helm/smartstock/templates/backup-cronjob.yaml).
#
# Dumps are laid out as:  s3://<bucket>/<db_name>/<db_name>-<UTC-timestamp>.sql.gz
#
# Usage:
#   scripts/db-restore.sh <db_name> [timestamp|latest]
#
# Examples:
#   scripts/db-restore.sh smartstock_inventory latest
#   scripts/db-restore.sh smartstock_identity  20260701T020000Z
#
# Required env:
#   S3_BUCKET                 target backup bucket (e.g. smartstock-prod-backups)
#   PGHOST                    DB host to restore INTO
#   PGUSER / PGPASSWORD       DB superuser/owner credentials
#   AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_DEFAULT_REGION
# Optional env:
#   S3_ENDPOINT               S3-compatible endpoint (MinIO); empty = AWS S3
#   PGPORT                    default 5432
#   DROP_EXISTING             "true" to DROP+CREATE the DB before restore (DANGER)
#
set -euo pipefail

DB_NAME="${1:?usage: db-restore.sh <db_name> [timestamp|latest]}"
WHEN="${2:-latest}"
: "${S3_BUCKET:?S3_BUCKET is required}"
: "${PGHOST:?PGHOST is required}"
: "${PGUSER:?PGUSER is required}"
PGPORT="${PGPORT:-5432}"
ENDPOINT_ARG=""
[ -n "${S3_ENDPOINT:-}" ] && ENDPOINT_ARG="--endpoint-url ${S3_ENDPOINT}"

prefix="s3://${S3_BUCKET}/${DB_NAME}/"

if [ "$WHEN" = "latest" ]; then
  key="$(aws $ENDPOINT_ARG s3 ls "$prefix" | sort | tail -1 | awk '{print $4}')"
  [ -n "$key" ] || { echo "No dumps found under $prefix" >&2; exit 1; }
else
  key="${DB_NAME}-${WHEN}.sql.gz"
fi

echo ">> Restoring ${DB_NAME} from ${prefix}${key} into ${PGHOST}:${PGPORT}"
read -rp "This will OVERWRITE data in ${DB_NAME}. Type the db name to confirm: " confirm
[ "$confirm" = "$DB_NAME" ] || { echo "Aborted."; exit 1; }

if [ "${DROP_EXISTING:-false}" = "true" ]; then
  echo ">> Dropping and recreating ${DB_NAME}"
  psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres \
    -c "DROP DATABASE IF EXISTS ${DB_NAME} WITH (FORCE);" \
    -c "CREATE DATABASE ${DB_NAME};"
fi

aws $ENDPOINT_ARG s3 cp "${prefix}${key}" - \
  | gunzip \
  | psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$DB_NAME" -v ON_ERROR_STOP=1

echo ">> Restore of ${DB_NAME} complete."
echo ">> Flyway will reconcile schema history on next service start (baseline-on-migrate)."
