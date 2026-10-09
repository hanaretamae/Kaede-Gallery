#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf 'error: %s\n' "$1" >&2
  exit 1
}

if [[ $# -lt 1 || $# -gt 2 ]]; then
  fail "usage: tools/run-optional-builds.sh {android-debug|linux-aarch64|both} [ref]"
fi

target=$1
ref=${2:-main}
case "$target" in
  android-debug|linux-aarch64|both) ;;
  *) fail "unknown target '$target'; choose android-debug, linux-aarch64, or both" ;;
esac
[[ -n $ref && $ref != -* ]] || fail "ref must be a branch or tag name"

command -v gh >/dev/null || fail "GitHub CLI (gh) is required; enter nix develop first"
gh auth status --hostname github.com >/dev/null 2>&1 ||
  fail "authenticate with gh auth login first"

gh workflow run optional-arm-builds.yml \
  --repo hanaretamae/Kaede-Gallery \
  --ref "$ref" \
  --field "target=$target"

printf 'Dispatched target=%s ref=%s\n' "$target" "$ref"
printf 'Check progress with: gh run list --workflow optional-arm-builds.yml --repo hanaretamae/Kaede-Gallery\n'
