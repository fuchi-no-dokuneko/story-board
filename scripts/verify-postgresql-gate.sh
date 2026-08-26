#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
decision="$root/docs/persistence/postgresql-trigger.json"

jq -e '
  .schema_version == "postgresql-trigger-1.0.0"
  and .status == "not_triggered"
  and .implementation_allowed == false
  and ([.approved_requirements[]] | any) == false
  and .evidence.durable_rows == .evidence.attempted_rows
' "$decision" >/dev/null

if find "$root/modules" -maxdepth 1 -type d -iname '*postgres*' | grep -q .; then
  echo "PostgreSQL adapter exists before a trigger was approved" >&2
  exit 1
fi
if find "$root" -name pom.xml -type f -exec grep -l \
    '<artifactId>postgresql</artifactId>' {} + | grep -q .; then
  echo "PostgreSQL JDBC dependency exists before a trigger was approved" >&2
  exit 1
fi

echo "PostgreSQL gate is deferred and no premature adapter is present"
