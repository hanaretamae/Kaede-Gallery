# Release and build guide

`VERSION` is the single application version source. Official releases use a tag matching it exactly, without a `v` prefix. Beta tags such as `2.0.0b3` become GitHub prereleases whose title is the version only.

## Choose a route

| Route | What it produces | Where it runs |
| --- | --- | --- |
| Full release (recommended for 2.0.0b3) | Production-signed universal, arm64-v8a, and x86_64 Android APKs; x86_64 and AArch64 Linux bundles; checksums; and a GitHub prerelease | Local Linux host through `tools/release.sh`; Windows ZIP is attached afterward by Actions |
| Optional remote builds | Android debug APK and/or Linux AArch64 bundle as temporary workflow artifacts | Manually dispatched Actions workflow; not a production release |
| Entirely Actions-based signed release | Not currently supported | No workflow currently handles Android production signing and creates the complete release |

## Current 2.0.0b3 status

The `2.0.0b3` release is being prepared and has not been published. Review and commit the changes, push the matching tag, and verify the target builds before running the release script. The prior `2.0.0b2` tag points to commit `6fa0080`; do not move or recreate it.

The release script permits Markdown-only documentation commits after the tag, except changes to `CHANGELOG.md`; it requires the current commit to match `origin/main`. Keep the release notes and all non-documentation release code at the tagged commit.

## Full release from a local Linux host

Use a clean checkout of the latest `main`, and confirm `VERSION` is `2.0.0b3`. The release script verifies the tag, branch, GitHub authentication, test prerequisites, signing files, Rust targets, and Nix builders before publishing.

```sh
git switch main
git pull --ff-only origin main
git fetch origin --tags
cat VERSION
git status --short
```

Configure paths to the KeePassXC database and key file locally. Do not place their contents in the repository, command arguments, or chat.

```sh
export KEEPASSXC_DATABASE="/absolute/path/to/signing-database.kdbx"
export KEEPASSXC_KEY_FILE="/absolute/path/to/signing-key-file"
tools/release.sh 2.0.0b3
```

The expected KeePassXC entry defaults to `Kaede Gallery Android signing`, with attachment `release.jks`. The script exports the key temporarily, builds and verifies the signed universal, arm64-v8a, and x86_64 APKs, runs the Rust and Kotlin tests, creates x86_64 and AArch64 Linux bundles, calculates checksums, and publishes a version-titled prerelease. It requires Rustup 1.98.1 with `aarch64-linux-android` and `x86_64-linux-android` targets already installed. Both Linux bundles are built on an x86_64 Linux builder; the AArch64 Rust FFI is cross-compiled and target-native runtime files are assembled without QEMU/binfmt or an AArch64/remote builder. `XDG_RUNTIME_DIR` must be a private directory owned by the current user with mode `700`.

After the prerelease exists, build and attach the Windows ZIP through the manual Windows workflow:

```sh
gh workflow run windows.yml \
  --repo hanaretamae/Kaede-Gallery \
  --ref main \
  --field tag=2.0.0b3
gh run list --repo hanaretamae/Kaede-Gallery --workflow windows.yml
```

The workflow verifies the tag and version, packages Windows x64, and uploads the ZIP and updated checksums to the existing release. It cannot create the release itself.

## Optional remote builds

For artifact-only Android/Linux ARM builds, enter the Nix development shell and use the CLI wrapper. The default ref is `main`; the optional second argument selects another branch or tag containing the workflow.

```sh
nix develop
tools/run-optional-builds.sh android-debug
tools/run-optional-builds.sh linux-aarch64
tools/run-optional-builds.sh both
```

Choose one target per invocation. The workflow also appears in the GitHub Actions UI as **Optional Android and Linux AArch64 builds**. It is manual-only, keeps local builds as the normal path, and uploads artifacts for 14 days. `android-debug` is a debug-signed APK, not the production APK; `linux-aarch64` cross-builds an AArch64 bundle on an x86_64 runner. This workflow does not build the x86_64 release bundle, create a GitHub Release, or sign/publish a production Android APK.

The separate `windows.yml` workflow can produce a Windows artifact without a tag, but it only attaches a ZIP when dispatched with a tag for an already-created GitHub Release.

## Entirely Actions-based production release

A complete Actions-only signed release is **not available yet**. The current production signing process reads the keystore and password from the release owner's local KeePassXC database. The optional build workflow only creates temporary debug/build artifacts, and the Windows workflow requires a release to exist first.

Supporting a full remote release would require a dedicated protected release workflow, a deliberate decision to provide the Android signing keystore/password through a protected GitHub Environment or an external secret manager, and CI verification of the signed APKs and all release bundles. Do not upload the KeePassXC database/key file or put signing material in repository files. This remote signing setup has not been approved or configured. For `2.0.0b3`, use the local release procedure above.
