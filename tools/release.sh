#!/usr/bin/env bash
set -euo pipefail
set +x

# Re-enter the pinned development shell so one command is enough.
if [[ -z ${ANDROID_HOME:-} && -z ${KAEDE_IN_NIX:-} ]]; then
  command -v nix >/dev/null || { echo "error: Nix is required" >&2; exit 1; }
  exec env KAEDE_IN_NIX=1 nix develop --command "$0" "$@"
fi

fail() {
  printf 'error: %s\n' "$1" >&2
  exit 1
}

if [[ $# -ne 1 || ! $1 =~ ^[0-9]+\.[0-9]+\.[0-9]+(b[0-9]+)?$ ]]; then
  fail "usage: tools/release.sh MAJOR.MINOR.PATCH[bN] (without a v prefix)"
fi

tag=$1
repo_root=$(git rev-parse --show-toplevel) || fail "run this inside the Git repository"
cd "$repo_root"

[[ -z $(git status --porcelain) ]] || fail "commit or stash all changes before releasing"
command -v gh >/dev/null || fail "GitHub CLI (gh) is required; enter nix develop first"
command -v python3 >/dev/null || fail "Python 3 is required; enter nix develop first"
command -v cargo >/dev/null || fail "Cargo is required; enter nix develop first"
command -v rustup >/dev/null || fail "Rustup is required; enter nix develop first"
command -v keepassxc-cli >/dev/null || fail "KeePassXC CLI (keepassxc-cli) is required"
[[ -n ${ANDROID_HOME:-} && -n ${ANDROID_NDK_HOME:-} ]] || fail "enter nix develop to configure the Android SDK"
gh auth status --hostname github.com >/dev/null 2>&1 || fail "authenticate with gh auth login first"

app_version=$(tr -d '\r\n' < VERSION)
[[ $tag == "$app_version" ]] || fail "tag must match VERSION $app_version (without a v prefix)"
git show-ref --verify --quiet "refs/tags/$tag" || fail "create the local tag first: git tag -a $tag -m $tag"
tag_commit=$(git rev-parse "$tag^{commit}")
head_commit=$(git rev-parse HEAD)
if [[ $tag_commit != "$head_commit" ]]; then
  git merge-base --is-ancestor "$tag_commit" "$head_commit" ||
    fail "the release tag must point to HEAD or an earlier commit"
  while IFS= read -r -d '' changed_path; do
    case $changed_path in
      CHANGELOG.md)
        fail "CHANGELOG.md must match the release tag"
        ;;
      *.md|tools/release.sh)
        ;;
      *)
        fail "only documentation changes may follow the release tag"
        ;;
    esac
  done < <(git diff --name-only -z "$tag_commit" "$head_commit")
fi

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
dist_cleanup=
cleanup() {
  [[ -z $dist_cleanup ]] || rm -rf -- "$dist_cleanup"
  [[ -z $notes_file ]] || rm -f -- "$notes_file"
  [[ -z $key_dir ]] || rm -rf -- "$key_dir"
  unset signing_password
}
trap cleanup EXIT
notes_file=$(mktemp)

awk -v version="$tag" '
  $0 ~ "^## \\[" version "\\]( - .*)?$" { found = 1; next }
  found && /^## / { exit }
  found { print }
' CHANGELOG.md > "$notes_file"
[[ -s $notes_file ]] || fail "CHANGELOG.md has no release notes for $tag"

if command -v nix >/dev/null; then
  nix_config=$(nix config show 2>/dev/null || true)
  native_system=$(awk '$1 == "system" { print $3 }' <<< "$nix_config")
  extra_systems=$(awk '$1 == "extra-platforms" { $1 = $2 = ""; print }' <<< "$nix_config")
  has_builders=$(awk '$1 == "builders" && NF > 2 { print "yes" }' <<< "$nix_config")
  for system in x86_64-linux aarch64-linux; do
    [[ $system == "$native_system" || " $extra_systems " == *" $system "* || -n $has_builders ]] ||
      fail "no way to build $system (needs native support, QEMU/binfmt, or a remote builder)"
  done
fi

python3 tools/generate_saf_limits.py --check
cargo test --locked --workspace
(
  cd kotlin
  ./gradlew --no-daemon \
    :core:rust:jvmTest \
    :core:settings:jvmTest \
    :ui:app:jvmTest \
    :desktopApp:test
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

rust_toolchain=1.98.1
rustup run "$rust_toolchain" rustc --version >/dev/null 2>&1 ||
  fail "Rustup toolchain $rust_toolchain is not installed"
for target in aarch64-linux-android x86_64-linux-android; do
  rustup target list --installed --toolchain "$rust_toolchain" | grep -Fxq "$target" ||
    fail "Rust target $target is not installed for Rustup $rust_toolchain; install it before releasing"
done
rustc_path=$(rustup which rustc --toolchain "$rust_toolchain") ||
  fail "Rustup could not locate rustc for $rust_toolchain"
rustdoc_path=$(rustup which rustdoc --toolchain "$rust_toolchain") ||
  fail "Rustup could not locate rustdoc for $rust_toolchain"
export RUSTUP_TOOLCHAIN=$rust_toolchain
export RUSTC=$rustc_path
export RUSTDOC=$rustdoc_path
export PATH="$(dirname "$rustc_path"):$PATH"
ndk_bin="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
[[ -x $ndk_bin/aarch64-linux-android23-clang && -x $ndk_bin/x86_64-linux-android23-clang ]] ||
  fail "Android NDK compiler was not found"
for triple in aarch64 x86_64; do
  upper=${triple^^}
  export "CC_${triple}_linux_android=$ndk_bin/${triple}-linux-android23-clang"
  export "CXX_${triple}_linux_android=$ndk_bin/${triple}-linux-android23-clang++"
  export "AR_${triple}_linux_android=$ndk_bin/llvm-ar"
  export "RANLIB_${triple}_linux_android=$ndk_bin/llvm-ranlib"
  export "CARGO_TARGET_${upper}_LINUX_ANDROID_LINKER=$ndk_bin/${triple}-linux-android23-clang"
done

dist_dir=$(mktemp -d "$XDG_RUNTIME_DIR/kaede-gallery-dist.XXXXXXXX") ||
  fail "could not create a release output directory"
dist_cleanup=$dist_dir
version=$tag
latest_build_tools=$(find -L "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -V | tail -n 1)
[[ -n $latest_build_tools ]] || fail "Android SDK build-tools were not found"
apksigner="$ANDROID_HOME/build-tools/$latest_build_tools/apksigner"
command -v apkanalyzer >/dev/null || fail "Android SDK apkanalyzer was not found"
[[ -x $apksigner ]] || fail "apksigner was not found"

(
  cd kotlin
  ANDROID_KEYSTORE_PATH="$keystore_path" \
    ANDROID_KEYSTORE_PASSWORD="$signing_password" \
    ANDROID_KEY_ALIAS=kaede-gallery \
    ANDROID_KEY_PASSWORD="$signing_password" \
    GRADLE_OPTS="${GRADLE_OPTS:+$GRADLE_OPTS }-Dorg.gradle.daemon=false" \
    ./gradlew --no-daemon :androidApp:assembleRelease
)
apk_dir=kotlin/androidApp/build/outputs/apk/release
declare -A apk_variants=(
  [universal]='*-universal-release.apk'
  [arm64-v8a]='*-arm64-v8a-release.apk'
  [x86_64]='*-x86_64-release.apk'
)
for arch in universal arm64-v8a x86_64; do
  mapfile -d '' apks < <(find "$apk_dir" -maxdepth 1 -type f -name "${apk_variants[$arch]}" -print0)
  [[ ${#apks[@]} -eq 1 ]] ||
    fail "expected exactly one $arch KMP release APK, found ${#apks[@]}"
  apk=${apks[0]}
  "$apksigner" verify "$apk" || fail "$arch APK signature verification failed"
  application_id=$(apkanalyzer manifest application-id "$apk") ||
    fail "could not inspect the $arch APK application ID"
  [[ $application_id == com.hanaretamae.kaede ]] ||
    fail "unexpected $arch KMP APK application ID: $application_id"
  permissions=$(apkanalyzer manifest permissions "$apk") ||
    fail "could not inspect the $arch APK permissions"
  if grep -Eq 'android\.permission\.(INTERNET|ACCESS_NETWORK_STATE|ACCESS_WIFI_STATE|CHANGE_NETWORK_STATE|CHANGE_WIFI_STATE|MANAGE_EXTERNAL_STORAGE|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|READ_MEDIA_IMAGES|READ_MEDIA_VIDEO|READ_MEDIA_AUDIO|READ_MEDIA_VISUAL_USER_SELECTED)' \
    <<< "$permissions"; then
    fail "the $arch KMP APK requests a network or broad-storage permission"
  fi
  cp -- "$apk" "$dist_dir/kaede-gallery-$version-android-$arch.apk"
done
unset signing_password
rm -f -- "$keystore_path"
rmdir -- "$key_dir"
key_dir=

# Self-contained Linux executables (no Nix needed on the target machine).
# aarch64-linux requires QEMU/binfmt or a remote builder on x86_64 hosts.
command -v nix >/dev/null || fail "Nix is required to build the Linux bundles"
for system in x86_64-linux aarch64-linux; do
  arch=${system%-linux}
  linux_out="$dist_dir/linux-$arch"
  nix bundle --system "$system" --out-link "$linux_out" ".#packages.$system.default" ||
    fail "could not build the $system bundle"
  [[ -f $linux_out ]] || fail "the $system bundle was not produced"
  cp -- "$(readlink -f "$linux_out")" "$dist_dir/kaede-gallery-$version-linux-$arch"
  rm -f -- "$linux_out"
  chmod 755 "$dist_dir/kaede-gallery-$version-linux-$arch"
done

cp -- LICENSE "$dist_dir/kaede-gallery-LICENSE.txt"
cp -- THIRD_PARTY_NOTICES.md "$dist_dir/kaede-gallery-THIRD_PARTY_NOTICES.md"
tar -C kotlin/shared-assets -czf "$dist_dir/kaede-gallery-third-party-licenses.tar.gz" licenses
(cd "$dist_dir" && sha256sum -- kaede-gallery-* > SHA256SUMS)

release_flags=()
if [[ $tag =~ b[0-9]+$ ]]; then
  release_flags+=(--prerelease)
fi
gh release create "$tag" "$dist_dir"/kaede-gallery-* "$dist_dir/SHA256SUMS" \
  --verify-tag \
  --title "$tag" \
  --notes-file "$notes_file" \
  --repo "$repo" \
  "${release_flags[@]}"

printf 'Released %s:\n' "$tag"
ls -1 -- "$dist_dir"
