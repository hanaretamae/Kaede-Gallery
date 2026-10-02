#!/usr/bin/env bash
set -euo pipefail
set +x

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
command -v keepassxc-cli >/dev/null || fail "KeePassXC CLI (keepassxc-cli) is required"
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

repo=$(gh repo view --json nameWithOwner --jq .nameWithOwner) || fail "could not identify the GitHub repository"
if gh release view "$tag" --repo "$repo" >/dev/null 2>&1; then
  fail "a GitHub Release for $tag already exists"
fi

keepassxc_database=${KEEPASSXC_DATABASE:-}
keepassxc_entry=${KEEPASSXC_ENTRY:-Kaede Gallery Android signing}
keepassxc_attachment=${KEEPASSXC_ATTACHMENT:-release.jks}
[[ -n $keepassxc_database && -f $keepassxc_database && -r $keepassxc_database ]] ||
  fail "set KEEPASSXC_DATABASE to a readable KeePassXC database"
[[ -n ${KEEPASSXC_KEY_FILE:-} && -f $KEEPASSXC_KEY_FILE && -r $KEEPASSXC_KEY_FILE ]] ||
  fail "set KEEPASSXC_KEY_FILE to a readable key file"
is_kdbx() {
  [[ $(head -c 4 -- "$1" | od -An -tx1 | tr -d ' \n') == 03d9a29a ]]
}
if ! is_kdbx "$keepassxc_database"; then
  if is_kdbx "$KEEPASSXC_KEY_FILE"; then
    fail "KEEPASSXC_DATABASE and KEEPASSXC_KEY_FILE look swapped"
  fi
  fail "KEEPASSXC_DATABASE is not a KeePass (.kdbx) database"
fi
keepassxc_auth=(--key-file "$KEEPASSXC_KEY_FILE")
[[ -n ${XDG_RUNTIME_DIR:-} && -d $XDG_RUNTIME_DIR && -O $XDG_RUNTIME_DIR ]] ||
  fail "XDG_RUNTIME_DIR must be a private runtime directory"
runtime_mode=$(stat -c '%a' -- "$XDG_RUNTIME_DIR") ||
  fail "could not inspect XDG_RUNTIME_DIR permissions"
[[ $runtime_mode == 700 ]] || fail "XDG_RUNTIME_DIR must have mode 700"

key_dir=
notes_file=
cleanup() {
  [[ -z $notes_file ]] || rm -f -- "$notes_file"
  [[ -z $key_dir ]] || rm -rf -- "$key_dir"
  unset signing_password
}
trap cleanup EXIT
notes_file=$(mktemp)

awk -v version="${tag#v}" '
  $0 ~ "^## \\[" version "\\]( - .*)?$" { found = 1; next }
  found && /^## / { exit }
  found { print }
' CHANGELOG.md > "$notes_file"
[[ -s $notes_file ]] || fail "CHANGELOG.md has no release notes for $tag"

rustup target add aarch64-linux-android
cargo test --locked --workspace
(
  cd app
  flutter pub get --enforce-lockfile
  flutter analyze
  flutter test
)

key_dir=$(mktemp -d "$XDG_RUNTIME_DIR/kaede-gallery-release.XXXXXXXX") ||
  fail "could not create a private release directory"
keystore_path="$key_dir/release.jks"
if ! keepassxc-cli attachment-export "${keepassxc_auth[@]}" \
  "$keepassxc_database" "$keepassxc_entry" "$keepassxc_attachment" "$keystore_path"; then
  fail "could not export the signing keystore from KeePassXC"
fi
[[ -s $keystore_path ]] || fail "KeePassXC exported an empty signing keystore"
chmod 600 "$keystore_path"

if ! signing_password=$(keepassxc-cli show "${keepassxc_auth[@]}" --show-protected --attributes Password \
  "$keepassxc_database" "$keepassxc_entry"); then
  fail "could not read the signing password from KeePassXC"
fi
[[ -n $signing_password ]] || fail "the KeePassXC signing entry has no password"

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
  ANDROID_KEYSTORE_PATH="$keystore_path" \
    ANDROID_KEYSTORE_PASSWORD="$signing_password" \
    ANDROID_KEY_ALIAS=kaede-gallery \
    ANDROID_KEY_PASSWORD="$signing_password" \
  GRADLE_OPTS="${GRADLE_OPTS:+$GRADLE_OPTS }-Dorg.gradle.daemon=false" \
  flutter build apk --release --target-platform android-arm64
)
unset signing_password
rm -f -- "$keystore_path"
rmdir -- "$key_dir"
key_dir=

apk="$repo_root/app/build/app/outputs/flutter-apk/app-release.apk"
apksigner="$ANDROID_HOME/build-tools/36.0.0/apksigner"
[[ -f $apk && -x $apksigner ]] || fail "APK or apksigner was not produced"
"$apksigner" verify "$apk" || fail "APK signature verification failed"

gh release create "$tag" "$apk" \
  --verify-tag \
  --title "$tag" \
  --notes-file "$notes_file" \
  --repo "$repo"

printf 'Released %s: %s\n' "$tag" "$apk"
