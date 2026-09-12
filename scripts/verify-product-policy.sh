#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
python3 "$root/scripts/assemble-sources.py"
policy=$root/docs/adr/0008-initial-product-policy.notes.txt
for number in 01 02 03 04 05 06 07 08 09 10; do
  rg -q "^\| O-$number \|" "$policy"
done
echo 'Original product decisions retained / 原產品決策已保留 / 原产品决策已保留'
