#!/usr/bin/env bash
set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

update_script=$(nix build --no-link --print-out-paths \
  .#packages.x86_64-linux.kmpDesktop.mitmCache.updateScript)
temporary_script=$(mktemp "${TMPDIR:-/tmp}/kaede-gradle-deps.XXXXXXXX")
trap 'rm -f -- "$temporary_script"' EXIT

python3 - "$update_script" "$temporary_script" "$repo_root/kotlin/deps.json" <<'PY'
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
