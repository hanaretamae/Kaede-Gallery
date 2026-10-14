# Build and release guide

This is the maintainer procedure for building and publishing Kaede Gallery.
Keep release-specific status and asset lists in GitHub Release metadata or
versioned release notes; this guide describes the repeatable process.

[`docs/design.md`](design.md) is authoritative for architecture and project
constraints. Never use a user's Vault as a release or test fixture.

## Release routes

| Route | Output |
| --- | --- |
| Local production release with `tools/release.sh` | Signed universal, arm64-v8a, and x86_64 Android APKs; self-contained x86_64 and AArch64 Linux `.tar.zst` archives and AppImages; license materials, checksums, and a GitHub Release |
| Manual Windows workflow with a release tag | Windows x64 and ARM64 ZIPs attached to an existing GitHub Release, with updated checksums |
| Manual Windows workflow without a tag | Windows ZIP build artifacts without a GitHub Release attachment |
| Optional Android/Linux workflow | Android debug APK and/or an AArch64 Linux bundle as temporary Actions artifacts |

The release tag must match `VERSION` exactly and must not have a `v` prefix.
Tags with a beta (`bN`) or release-candidate (`rcN`) suffix are published as
prereleases. `tools/release.sh` reads release notes from the matching section
in `CHANGELOG.md`.

## Prepare a release

Only the release owner should authorize a release, update `VERSION`, create
tags, or publish artifacts. Before tagging:

- Review and update `CHANGELOG.md` from the actual implementation and verified
  results.
- Run the Rust and Kotlin checks required by the changes. The release script
  also runs its required test suites and safety checks.
- Commit the reviewed changes to `main`, push them, and confirm the worktree is
  clean. The release script requires the commit to match `origin/main`.
- Confirm `VERSION` and the release-note heading agree. Do not reuse a
  previously published tag.

For example, from a clean checkout after the release owner has approved the
version and changelog:

```sh
git switch main
git pull --ff-only origin main
git fetch origin --tags
cat VERSION
git status --short
```

## Publish Android and Linux artifacts

Use a Linux host with the pinned Nix development environment, authenticated
GitHub CLI, KeePassXC CLI, Android SDK/NDK, and Rustup 1.98.1. The Android
targets `aarch64-linux-android` and `x86_64-linux-android` must already be
installed for that toolchain. The release script runs the Rust workspace
tests, Kotlin/JVM tests, Android release assembly and validation, and builds
both Linux architectures. The AArch64 Linux package is cross-built from an
x86_64 Linux host.

Enter the development shell and point the script to the local KeePassXC
database and key file. Do not put their contents or signing credentials in the
repository, command arguments, or chat.

```sh
nix develop
export KEEPASSXC_DATABASE="/absolute/path/to/signing-database.kdbx"
export KEEPASSXC_KEY_FILE="/absolute/path/to/signing-key-file"
release_tag="$(cat VERSION)"
```

The release script uses the KeePassXC entry `Kaede Gallery Android signing`
and attachment `release.jks` by default. It requires `XDG_RUNTIME_DIR` to be a
private directory owned by the current user with mode `700`; temporary signing
material is stored there and removed by the script.

Create and push the annotated tag only after the reviewed release commit is
already on `origin/main`. The script verifies the local and remote tag, source
commit, credentials, release notes, and build prerequisites before publishing.

```sh
git tag -a "$release_tag" -m "$release_tag"
git push origin main "$release_tag"
tools/release.sh "$release_tag"
```

The script creates the GitHub Release and its Android/Linux assets. After it
finishes, start the Windows workflow to build both Windows architectures and
attach their ZIPs:

```sh
gh workflow run windows.yml \
  --repo hanaretamae/Kaede-Gallery \
  --ref main \
  --field tag="$release_tag"
gh run list --repo hanaretamae/Kaede-Gallery --workflow windows.yml
```

The Windows workflow requires the release to exist first. It verifies that
the tag matches `VERSION`, packages `Kaede-Gallery.exe` with the project and
third-party notices, and updates the release checksums.

## Optional artifact builds

The optional Android/Linux workflow is manually dispatched; it does not run
for pushes or pull requests and does not sign production APKs or publish
releases. It can build an Android debug APK, an AArch64 Linux bundle, or both.
Artifacts are temporary and retained for 14 days.

From `nix develop`, use:

```sh
tools/run-optional-builds.sh android-debug
tools/run-optional-builds.sh linux-aarch64
tools/run-optional-builds.sh both
```

An optional second argument selects the workflow ref. The authenticated GitHub
CLI is required.

The manual Windows workflow can also be dispatched without a tag to build
ZIP artifacts only. Its artifacts are retained for seven days. Supplying a
tag attaches the Windows ZIPs to an existing release.

## Verify publication

Do not describe a release as published based on a tag, workflow definition, or
successful build alone. Inspect the public GitHub Release and verify its
actual assets and checksums:

```sh
gh release view "$release_tag" --repo hanaretamae/Kaede-Gallery
verify_dir="$(mktemp -d)"
gh release download "$release_tag" \
  --repo hanaretamae/Kaede-Gallery \
  --dir "$verify_dir"
(cd "$verify_dir" && sha256sum -c SHA256SUMS)
```

Review the downloaded asset names and contents against the release notes and
intended targets. Remove the temporary verification directory afterward.

## Signing boundary

Production Android signing is performed locally by `tools/release.sh`, which
retrieves credentials from the release owner's KeePassXC database. The
Actions workflows do not perform the complete production signing and
publication process. Never upload the KeePassXC database or key file, or
store signing material in repository files.
