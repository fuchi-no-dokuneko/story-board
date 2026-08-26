#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
policy="$root/docs/adr/0008-initial-product-policy.md"

grep -q '^- Policy version: product-policy-1.0.0$' "$policy"
for number in 01 02 03 04 05 06 07 08 09 10; do
  row=$(grep -E "^\| O-$number \|" "$policy")
  [[ $(awk -F'|' '{print NF}' <<< "$row") -eq 8 ]]
  [[ "$row" == *"| fuchi-no-dokuneko | 2026-08-22 |"* ]]
  [[ "$row" =~ \|[[:space:]][^|]+-[0-9]+\.[0-9]+\.[0-9]+[^|]*\|$ ]]
done

echo "O-01 through O-10 are complete in product-policy-1.0.0"
