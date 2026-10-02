#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf 'error: %s\n' "$1" >&2
  exit 1
}

if [[ $# -ne 1 || ! $1 =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  fail "usage: tools/release-android.sh vMAJOR.MINOR.PATCH"
fi

tag=$1
repo_root=$(git rev-parse --show-toplevel) || fail "run this inside the Git repository"
cd "$repo_root"

[[ -z $(git status --porcelain) ]] || fail "commit or stash all changes before releasing"
command -v gh >/dev/null || fail "GitHub CLI (gh) is required; enter nix develop first"
command -v flutter >/dev/null || fail "Flutter is required; enter nix develop first"
command -v cargo >/dev/null || fail "Cargo is required; enter nix develop first"
command -v rustup >/dev/null || fail "Rustup is required; enter nix develop first"
[[ -n ${ANDROID_HOME:-} && -n ${ANDROID_NDK_HOME:-} ]] || fail "enter nix develop to configure the Android SDK"
gh auth status --hostname github.com >/dev/null 2>&1 || fail "authenticate with gh auth login first"

app_version=$(sed -n 's/^version:[[:space:]]*//p' app/pubspec.yaml | head -n 1 | cut -d+ -f1)
[[ $tag == "v$app_version" ]] || fail "tag must match app/pubspec.yaml version v$app_version"
git show-ref --verify --quiet "refs/tags/$tag" || fail "create the local tag first: git tag -a $tag -m $tag"
tag_commit=$(git rev-parse "$tag^{commit}")
head_commit=$(git rev-parse HEAD)
[[ $tag_commit == "$head_commit" ]] || fail "the tag must point to the current HEAD"

origin_main=$(git ls-remote origin refs/heads/main | awk 'NR == 1 { print $1 }')
[[ -n $origin_main && $origin_main == "$head_commit" ]] || fail "push this commit to origin/main before releasing"
remote_tag=$(git ls-remote origin "refs/tags/$tag^{}" "refs/tags/$tag" |
  awk -v peeled="refs/tags/$tag^{}" -v direct="refs/tags/$tag" '
    $2 == peeled { print $1; found = 1; exit }
    $2 == direct { value = $1 }
    END { if (!found && value != "") print value }
  ')
[[ $remote_tag == "$tag_commit" ]] || fail "push tag $tag to origin before releasing"

for variable in ANDROID_KEYSTORE_PATH ANDROID_KEYSTORE_PASSWORD ANDROID_KEY_ALIAS ANDROID_KEY_PASSWORD; do
  [[ -n ${!variable:-} ]] || fail "$variable must be set for a consistently signed release"
done
[[ -f $ANDROID_KEYSTORE_PATH ]] || fail "ANDROID_KEYSTORE_PATH does not point to a file"

repo=$(gh repo view --json nameWithOwner --jq .nameWithOwner) || fail "could not identify the GitHub repository"
if gh release view "$tag" --repo "$repo" >/dev/null 2>&1; then
  fail "a GitHub Release for $tag already exists"
fi

rustup target add aarch64-linux-android
cargo test --locked --workspace
(
  cd app
  flutter pub get --enforce-lockfile
  flutter analyze
  flutter test
)

ndk_bin="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
[[ -x $ndk_bin/aarch64-linux-android35-clang ]] || fail "Android NDK compiler was not found"
rust_toolchain_bin=$(dirname "$(rustup which rustc)")
export PATH="$rust_toolchain_bin:$PATH"
export CC_aarch64_linux_android="$ndk_bin/aarch64-linux-android35-clang"
export CXX_aarch64_linux_android="$ndk_bin/aarch64-linux-android35-clang++"
export AR_aarch64_linux_android="$ndk_bin/llvm-ar"
export RANLIB_aarch64_linux_android="$ndk_bin/llvm-ranlib"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$CC_aarch64_linux_android"

(
  cd app
  flutter build apk --release --target-platform android-arm64
)

apk="$repo_root/app/build/app/outputs/flutter-apk/app-release.apk"
apksigner="$ANDROID_HOME/build-tools/36.0.0/apksigner"
[[ -f $apk && -x $apksigner ]] || fail "APK or apksigner was not produced"
"$apksigner" verify "$apk" || fail "APK signature verification failed"

gh release create "$tag" "$apk" \
  --verify-tag \
  --title "$tag" \
  --generate-notes \
  --repo "$repo"

printf 'Released %s: %s\n' "$tag" "$apk"
