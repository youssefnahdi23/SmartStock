#!/usr/bin/env bash
#
# check-coverage.sh — aggregate JaCoCo line coverage across every module and fail
# the build if it falls below a threshold (default 90%).
#
# The reactor produces one jacoco.xml per module (services/<svc>/target/site/jacoco/
# jacoco.xml); there is no aggregate report module (debt L-3). This script sums the
# LINE counters from every report it finds and computes an overall covered ratio, so
# the pipeline can enforce a single project-wide gate.
#
# Usage:
#   scripts/check-coverage.sh [MIN_PERCENT] [REPORT_ROOT]
#     MIN_PERCENT   integer/float percent, default 90
#     REPORT_ROOT   directory to scan for jacoco.xml, default: current dir
#
# Exit 0 = coverage >= threshold. Exit 1 = below threshold or no reports found.

set -euo pipefail

min="${1:-90}"
root="${2:-.}"

covered_total=0
missed_total=0
found=0

# Each JaCoCo report ends with report-level <counter type="LINE" missed=".." covered=".."/>
# lines (one per counter type). We grab only the LINE counters. To avoid double counting
# nested <counter> elements, we read the report-level totals: the last LINE counter in each
# file is the report aggregate.
while IFS= read -r report; do
  found=1
  line=$(grep -oE '<counter type="LINE" missed="[0-9]+" covered="[0-9]+"/>' "$report" | tail -n 1 || true)
  [[ -z "$line" ]] && continue
  missed=$(echo "$line" | sed -E 's/.*missed="([0-9]+)".*/\1/')
  covered=$(echo "$line" | sed -E 's/.*covered="([0-9]+)".*/\1/')
  missed_total=$((missed_total + missed))
  covered_total=$((covered_total + covered))
  ratio=$(awk -v c="$covered" -v m="$missed" 'BEGIN { t=c+m; printf (t>0)? "%.1f" : "n/a", (t>0)? 100*c/t : 0 }')
  echo "  $(dirname "$(dirname "$(dirname "$report")")") — ${ratio}% (${covered}/$((covered + missed)) lines)"
done < <(find "$root" -type f -path '*/site/jacoco/jacoco.xml' -not -path '*/dependency-check*/*')

if [[ "$found" -eq 0 ]]; then
  echo "FAIL: no jacoco.xml reports found under '$root'. Did the test jobs run?"
  exit 1
fi

total=$((covered_total + missed_total))
if [[ "$total" -eq 0 ]]; then
  echo "FAIL: JaCoCo reports contained zero measurable lines."
  exit 1
fi

pct=$(awk -v c="$covered_total" -v t="$total" 'BEGIN { printf "%.2f", 100*c/t }')
echo "----------------------------------------------------------------"
echo "Aggregate line coverage: ${pct}% (${covered_total}/${total} lines)"
echo "Required minimum:        ${min}%"

pass=$(awk -v p="$pct" -v m="$min" 'BEGIN { print (p+0 >= m+0) ? 1 : 0 }')
if [[ "$pass" -eq 1 ]]; then
  echo "PASS: coverage gate satisfied."
  exit 0
fi

echo "::error::Coverage gate FAILED — ${pct}% is below the required ${min}%."
exit 1
