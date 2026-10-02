#!/usr/bin/env bash
set -euo pipefail

notes="${1:-7806}"
work="${2:-/tmp/vault-gallery-benchmark-${notes}}"
vault="${work}/vault"
database="${work}/index.sqlite"

mkdir -p "$work"
cargo run --release -q -p gallery-cli --bin generate-dummy-vault -- "$vault" --notes "$notes"
cargo build --release -p gallery-cli
echo "initial scan: notes=$notes"
time target/release/gallery-cli scan "$vault" --database "$database"
echo "unchanged rescan: notes=$notes"
time target/release/gallery-cli scan "$vault" --database "$database"
echo "category query: notes=$notes"
time target/release/gallery-cli categories "$vault" --database "$database" >/dev/null
echo "broad tag-filtered query: notes=$notes"
time target/release/gallery-cli list "$vault" --database "$database" source/service/example >/dev/null
echo "narrow tag-filtered query: notes=$notes"
time target/release/gallery-cli list "$vault" --database "$database" source/type/human >/dev/null
