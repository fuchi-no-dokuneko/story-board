#!/usr/bin/env bash
set -euo pipefail

output=${1:-artifacts/evaluations/ADR-317.json}
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"

timeout 60s ./mvnw -q \
  -pl modules/style,modules/rewrite-policy -am test \
  -Dtest=StyleAnomalyPolicyTest,RewritePolicyEvaluationTest \
  -Dsurefire.failIfNoSpecifiedTests=false

mkdir -p "$(dirname "$output")"
jq -s \
  --arg generated_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  '{schema_version:"adr-317-evaluation-1",generated_at:$generated_at,status:"pass",style:.[0],rewrite:.[1]}' \
  modules/style/target/evaluations/style-policy.json \
  modules/rewrite-policy/target/evaluations/rewrite-policy.json > "$output"
echo "$output"
