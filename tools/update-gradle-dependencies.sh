#!/usr/bin/env bash
set -euo pipefail

system="${1:-x86_64-linux}"
case "$system" in
  x86_64-linux|aarch64-linux) ;;
  *)
    echo "usage: $0 [x86_64-linux|aarch64-linux]" >&2
    exit 2
    ;;
esac

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

update_script=$(nix build --system "$system" --no-link --print-out-paths \
  ".#packages.${system}.kmpDesktop.mitmCache.updateScript")
temporary_dir=$(mktemp -d "${TMPDIR:-/tmp}/kaede-gradle-deps.XXXXXXXX")
temporary_script="$temporary_dir/update.sh"
generated_data="$temporary_dir/deps.json"
trap 'rm -rf -- "$temporary_dir"' EXIT

python3 - "$update_script" "$temporary_script" "$generated_data" <<'PY'
import re
import sys
from pathlib import Path

source = Path(sys.argv[1]).read_text(encoding="utf-8")
match = re.search(r'^outPath="([^"]+)"$', source, re.MULTILINE)
if match is None:
    raise SystemExit("error: Nix Gradle updater has no output path")
source = source[: match.start(1)] + sys.argv[3] + source[match.end(1) :]
Path(sys.argv[2]).write_text(source, encoding="utf-8")
PY
chmod 700 "$temporary_script"
"$temporary_script"

python3 - "$repo_root/kotlin/deps.json" "$generated_data" <<'PY'
import json
import os
import sys
import tempfile
from pathlib import Path


def merge(existing, incoming, path="deps.json"):
    if isinstance(existing, dict) and isinstance(incoming, dict):
        result = dict(existing)
        for key, value in incoming.items():
            child_path = f"{path}.{key}"
            if key in result:
                result[key] = merge(result[key], value, child_path)
            else:
                result[key] = value
        return result
    if existing == incoming:
        return existing
    raise SystemExit(f"error: conflicting Gradle dependency lock entry at {path}")


manifest_path, generated_path = map(Path, sys.argv[1:])
existing = json.loads(manifest_path.read_text(encoding="utf-8"))
incoming = json.loads(generated_path.read_text(encoding="utf-8"))
merged = merge(existing, incoming)
with tempfile.TemporaryDirectory(
    prefix=".gradle-deps-", dir=manifest_path.parent
) as temporary_dir:
    merged_path = Path(temporary_dir) / "deps.json"
    merged_path.write_text(
        json.dumps(merged, indent=1, sort_keys=True) + "\n", encoding="utf-8"
    )
    os.replace(merged_path, manifest_path)
PY
