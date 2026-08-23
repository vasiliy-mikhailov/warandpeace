#!/bin/sh
# RE-TAKE THE FIXTURES FROM A RUNNING SERVER. Read-only GETs, nothing written but the fixtures.
set -eu
base="${1:-http://127.0.0.1:8087}"
cd "$(dirname "$0")"
mkdir -p fixtures
for ep in manifest health items characters chapters badges; do
    curl -fsS "$base/api/$ep" > "fixtures/$ep.json"
done
curl -fsS "$base/api/items/pierre/book-one" > fixtures/item-detail.json
echo "captured from $base into $(pwd)/fixtures"
