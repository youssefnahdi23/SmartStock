#!/usr/bin/env bash
#
# check-flyway-versions.sh — fail the build on duplicate Flyway migration versions.
#
# Flyway aborts startup with "Found more than one migration with version N" when a
# service ships two migrations sharing a version (debt item C-1). This portable check
# catches that at build time — no live database required — so the defect fails CI, not
# the deploy.
#
# Scans every services/*/src/main/resources/db/migration directory, extracts the
# version token from each versioned migration (V<version>__description.sql), and reports
# any version that appears more than once within a single service.
#
# Exit 0 = all clear. Exit 1 = at least one duplicate found.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
status=0

while IFS= read -r dir; do
  service="$(echo "$dir" | sed -E 's#.*/services/([^/]+)/.*#\1#')"

  # Collect "version filename" for each versioned migration (skip repeatable R__).
  versions=$(find "$dir" -maxdepth 1 -name 'V*__*.sql' -printf '%f\n' 2>/dev/null \
    | sed -E 's/^V([0-9]+(\.[0-9]+)*)__.*/\1/' | sort)

  dupes=$(echo "$versions" | grep -v '^$' | uniq -d || true)
  if [[ -n "$dupes" ]]; then
    status=1
    while IFS= read -r v; do
      [[ -z "$v" ]] && continue
      echo "ERROR [$service]: duplicate Flyway version V$v:"
      find "$dir" -maxdepth 1 -name "V${v}__*.sql" -printf '  - %p\n'
    done <<< "$dupes"
  fi
done < <(find "$repo_root/services" -type d -path '*/src/main/resources/db/migration' -not -path '*/target/*')

if [[ "$status" -eq 0 ]]; then
  echo "OK: no duplicate Flyway migration versions found."
else
  echo "FAIL: duplicate Flyway migration versions detected (see above)."
fi
exit "$status"
